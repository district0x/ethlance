(ns district.graphql-utils
  (:require
   [bignumber.core :as bn]
   [camel-snake-kebab.core :as camel-snake]
   [camel-snake-kebab.extras :as camel-snake-extras]
   [cljs-time.coerce :as tc]
   [cljs-time.core :as t]
   [clojure.string :as string]
   [clojure.walk :as walk]
   [district.cljs-utils :refer [js-obj->clj kw->str]]
   ["graphql" :as GraphQL]))

; (def GraphQL (if (exists? js/GraphQL)
;                js/GraphQL
;                (require "graphql")))


(defn kw->gql-name
  "From namespaced keyword to GQL name:
  :videos.order-by/created-on -> videos_orderBy_createdOn"
  [kw]
  (let [nm (name kw)]
    (if (#{"ID" "ID!"} nm)
      nm
      (str
       (when (string/starts-with? nm "__")
         "__")
       (when (and (keyword? kw)
                  (namespace kw))
         (str (string/replace (camel-snake/->camelCase (namespace kw)) "." "_") "_"))
       (let [first-letter (first nm)
             last-letter (last nm)
             s (if (and (not= first-letter "_")
                        (= first-letter (string/upper-case first-letter)))
                 (camel-snake/->PascalCase nm)
                 (camel-snake/->camelCase nm))]
         (if (= last-letter "?")
           (.slice s 0 -1)
           s))
       (when (string/ends-with? nm "?")
         "_")))))


(defn gql-name->kw [gql-name]
  (when gql-name
    (let [k (name gql-name)]
      (if (string/starts-with? k "__")
        (keyword k)
        (let [k (if (string/ends-with? k "_")
                  (str (.slice k 0 -1) "?")
                  k)
              parts (string/split k "_")
              parts (if (< 2 (count parts))
                      [(string/join "." (butlast parts)) (last parts)]
                      parts)]
          (apply keyword (map camel-snake/->kebab-case parts)))))))


(defn clj->gql
  [m]
  (->> m
       (camel-snake-extras/transform-keys kw->gql-name)
       (clj->js)))


(defn gql->clj [m]
  (->> m
       (js->clj)
       (camel-snake-extras/transform-keys gql-name->kw )))


(defn gql-input->clj [input]
  (reduce (fn [result field]
            (assoc result (gql-name->kw field) (aget input field)))
          {}
          (js-keys input)))

(defn clj->js-root-value [root-value & [opts]]
  (let [gql-name->kw (or (:gql-name->kw opts) gql-name->kw)
        kw->gql-name (or (:kw->gql-name opts) kw->gql-name)]

    (cond
      (map? root-value)
      (clj->js (into {} (map (fn [[k v]]
                               [(kw->gql-name k)
                                (cond
                                  (fn? v)
                                  (fn [params context schema]
                                    (let [parsed-params (camel-snake-extras/transform-keys gql-name->kw (js->clj params))
                                          result (clj->js-root-value (v parsed-params context schema))]
                                      result))

                                  :else v)])
                             root-value))
               :keyword-fn identity)

      (sequential? root-value)
      (clj->js (map clj->js-root-value root-value) :keyword-fn identity)

      (instance? js/Promise root-value)
      (.then root-value clj->js-root-value)

      :else root-value)))


(defn js->clj-objects [res]
  (walk/prewalk (fn [x]
                  (if (and (nil? (type x))
                           (seq (js-keys x)))
                    (js-obj->clj x)
                    (js->clj x)))
                (js->clj res :keywordize-keys true)))


(defn js->clj-response [res & [opts]]
  (let [gql-name->kw (or (:gql-name->kw opts) gql-name->kw)
        resp (js->clj-objects res)]
    (update resp :data #(camel-snake-extras/transform-keys gql-name->kw %))))


(defn add-fields-to-schema-types [schema-ast fields]
  (let [query-type (js-invoke schema-ast "getQueryType")
        type-map (js-invoke schema-ast "getTypeMap")]
    (doseq [type-key (js-keys type-map)]
      (let [gql-type (aget type-map type-key)]
        (when (and (instance? (aget GraphQL "GraphQLObjectType") gql-type)
                   (not= query-type gql-type)
                   (nil? (aget gql-type "_typeConfig" "isIntrospection"))
                   (nil? (aget gql-type "_fields" "id")))
          (doseq [field fields]
            (aset gql-type "_fields" (:name field) (clj->js field)))))))
  schema-ast)


(def keyword-scalar-type-config
  {:name "Keyword"
   :description "Clojure Keyword"
   :serialize (fn [value]
                (if (keyword? value)
                  (kw->str value)
                  value))
   :parseValue (fn [value]
                 (keyword value))
   :parseLiteral (fn [ast]
                   (keyword (aget ast "value")))})


(def date-scalar-type-config
  {:name "Date"
   :description "goog.closure Date"
   :serialize (fn [value]
                (if (t/date? value)
                  (tc/to-long value)
                  value))
   :parseValue (fn [value]
                 (tc/from-long value))
   :parseLiteral (fn [ast]
                   (tc/from-long (aget ast "value")))})

(def bignumber-scalar-type-config
  {:name "BigNumber"
   :description "bignumber.js"
   :serialize (fn [value]
                (if (bn/bignumber? value)
                  (.toString value)
                  value))
   :parseValue (fn [value]
                 (bn/number value))
   :parseLiteral (fn [ast]
                   (bn/number (aget ast "value")))})


(defn add-scalar-type [schema-ast {:keys [:name :description :serialize :parseValue :parseLiteral]
                                   :or {serialize identity
                                        parseValue identity
                                        parseLiteral identity}
                                   :as scalar-type-config}]
  (if (nil? (aget schema-ast "_typeMap" name))
    (aset schema-ast "_typeMap" name (new (aget GraphQL "GraphQLScalarType")
                                          (clj->js scalar-type-config)))
    (let [keyword-type (or (aget schema-ast "_typeMap" name "_scalarConfig") (aget schema-ast "_typeMap" name))]
      (aset keyword-type "parseValue" parseValue)
      (aset keyword-type "parseLiteral" parseLiteral)
      (aset keyword-type "serialize" serialize)))
  schema-ast)


(defn add-keyword-type [schema-ast & [{:keys [:disable-serialize?]}]]
  (add-scalar-type schema-ast (cond-> keyword-scalar-type-config
                                disable-serialize? (dissoc :serialize))))


(defn add-date-type [schema-ast & [{:keys [:disable-serialize?]}]]
  (add-scalar-type schema-ast (cond-> date-scalar-type-config
                                disable-serialize? (dissoc :serialize))))


(defn add-bignumber-type [schema-ast & [{:keys [:disable-serialize?]}]]
  (add-scalar-type schema-ast (cond-> bignumber-scalar-type-config
                                disable-serialize? (dissoc :serialize))))


(defn gql-date->date
  "parse GraphQL Date type as JS Date object ready to be formatted"
  [gql-date]
  (tc/from-long (* 1000 gql-date)))
