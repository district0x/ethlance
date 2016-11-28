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
   :user/created-on uint
   :user/status uint8
   :user/freelancer? bool
   :user/employer? bool
   :user/languages-count uint})

(def freelancer-schema
  {:freelancer/available? bool
   :freelancer/job-title bytes32
   :freelancer/hourly-rate uint
   :freelancer/description string
   :freelancer/skills-count uint
   :freelancer/categories-count uint
   :freelancer/job-actions-count uint
   :freelancer/contracts-count uint
   :freelancer/avg-rating uint8
   :freelancer/total-earned uint})

(def job-schema
  {:job/employer uint
   :job/title string
   :job/description string
   :job/language uint
   :job/budget uint
   :job/created-on uint
   :job/category uint8
   :job/payment-type uint8
   :job/experience-level uint8
   :job/estimated-duration uint8
   :job/hours-per-week uint8
   :job/freelancers-needed uint8
   :job/status uint8
   :job/skills-count uint
   :job/proposals-count uint
   :job/contracts-count uint
   :job/invitations-count uint
   :job/hiring-done-on uint
   :job/total-paid uint})

(def employer-schema
  {:employer/description string
   :employer/jobs-count uint
   :employer/avg-rating uint8
   :employer/total-paid uint})

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

(def search-freelancers-args
  [:search/category :search/skills :search/min-avg-rating :search/min-contracts-count :search/min-hourly-rate
   :search/max-hourly-rate :search/country :search/language :search/offset :search/limit])

(def add-job-args
  [:job/title :job/description :job/skills :job/language :job/budget])

(def add-job-nested-args
  [:job/category :job/payment-type :job/experience-level :job/estimated-duration :job/hours-per-week
   :job/freelancers-needed])

(def search-jobs-args
  [:search/category :search/skills :search/payment-types :search/experience-levels :search/estimated-durations
   :search/hours-per-weeks :search/min-budget :search/min-employer-avg-rating :search/country :search/language
   :search/offset :search/limit])

(def schema
  (merge
    user-schema
    freelancer-schema
    employer-schema
    job-schema
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

(defn uint->value [val val-type]
  (condp = val-type
    bool (if (= val 0) false true)
    bytes32 (web3/to-ascii (web3/from-decimal val))
    val))

(defn parse-entities [fields result]
  (reduce (fn [acc [field-index results-of-field]]
            (reduce (fn [acc [item-index result-field]]
                      (let [field-name (nth fields field-index)]
                        (assoc-in acc
                                  [item-index field-name]
                                  (uint->value result-field (schema field-name)))))
                    acc (medley/indexed results-of-field)))
          [] (medley/indexed result)))

(defn log-entities [fields err res]
  (if err
    (console :error err)
    (console :log (parse-entities fields res))))

(defn get-entity [id fields instance]
  (let [records (map #(u/sha3 % id) fields)
        types (map schema fields)]
    (web3-eth/contract-call instance :get-entity records types (partial log-entity fields))))

(defn get-entity-list [ids fields instance]
  (let [records (flatten (for [id ids]
                           (for [field fields]
                             (u/sha3 field id))))
        types (map schema fields)]
    (web3-eth/contract-call instance :get-entity-list records types (partial log-entities fields))))

(comment
  (create-types-map [:a :b :c] [1 2 1])
  (get-entity 1 [:user/address :user/name :user/gravatar :user/country :user/status :user/freelancer?
                 :user/employer? :user/employer? :freelancer/available? :freelancer/job-title
                 :freelancer/hourly-rate :freelancer/description :employer/description]
              (get-in @re-frame.db/app-db [:contracts :ethlance-db :instance])))