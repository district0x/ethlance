(ns ethlance.shared.graphql.utils
  (:require [camel-snake-kebab.core :as camel-snake]
            [camel-snake-kebab.extras :as camel-snake-extras]
            [clojure.string :as string]))

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

(defn clj->gql [m]
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
