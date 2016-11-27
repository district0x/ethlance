(ns ethlance.ethlance-db
  (:refer-clojure :exclude [int])
  (:require
    [cljs-web3.eth :as web3-eth]
    [ethlance.utils :as u]
    [re-frame.core :refer [console dispatch]]
    [medley.core :as medley]
    [cljs-web3.core :as web3]))

(def bool 1)
(def uint8 2)
(def uint 3)
(def addr 4)
(def bytes32 5)
(def int 6)
(def string 7)

(def user-schema
  {:user/address addr
   :user/name bytes32
   :user/gravatar bytes32
   :user/country uint
   :user/status uint8
   :user/freelancer? bool
   :user/employer? bool})

(def freelancer-schema
  {:freelancer/available? bool
   :freelancer/job-title bytes32
   :freelancer/hourly-rate uint
   :freelancer/description string})

(def employer-schema
  {:employer/description string})

(def set-user-args
  [:user/name :user/gravatar :user/country :user/languages])

(def set-freelancer-args
  [:freelancer/available? :freelancer/job-title :freelancer/hourly-rate :freelancer/categories :freelancer/skills
   :freelancer/description])

(def register-freelancer-args
  (concat set-user-args set-freelancer-args))

(def set-employer-args
  [:employer/description])

(def register-employer-args
  (concat set-user-args set-employer-args))

(def schema
  (merge
    user-schema
    freelancer-schema
    employer-schema
    ))

(defn create-types-map [fields types]
  (reduce
    (fn [res [i field]]
      (update res (nth types i) (comp vec conj) field))
    {} (medley/indexed fields)))

(defn parse-value [val type]
  (if (= type bytes32)
    (web3/to-ascii val)
    val))

(defn parse-entity [fields result]
  (let [types (map schema fields)
        types-map (create-types-map fields types)]
    (reduce (fn [acc [i results-of-type]]
              (let [result-type (inc i)]
                (merge acc
                       (if (= result-type string)
                         {(get-in types-map [result-type 0]) results-of-type}
                         (into {}
                               (for [[j res] (medley/indexed results-of-type)]
                                 {(get-in types-map [result-type j])
                                  (parse-value res result-type)}))))))
            {} (medley/indexed result))))

(defn log-entity [fields err res]
  (if err
    (console :error err)
    (console :log (medley/map-vals u/big-num->num (parse-entity fields res)))))

(defn log-error [err]
  (console :error err))

(defn get-entity [id fields instance]
  (let [records (map #(u/sha3 % id) fields)
        types (map schema fields)]
    (web3-eth/contract-call instance :get-entity records types (partial log-entity fields))))


(comment
  (create-types-map [:a :b :c] [1 2 1])
  (get-entity 1 [:user/address :user/name :user/gravatar :user/country :user/status :user/freelancer?
                 :user/employer? :user/employer? :freelancer/available? :freelancer/job-title
                 :freelancer/hourly-rate :freelancer/description :employer/description]
              (get-in @re-frame.db/app-db [:contracts :ethlance-db :instance])))