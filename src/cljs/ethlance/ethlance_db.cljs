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
(def big-num 8)
(def uint-coll 9)

(def user-schema
  {:user/address addr
   :user/name bytes32
   :user/gravatar bytes32
   :user/country uint
   :user/created-on uint
   :user/status uint8
   :user/freelancer? bool
   :user/employer? bool
   :user/languages-count uint
   :user/languages uint-coll})

(def freelancer-schema
  {:freelancer/available? bool
   :freelancer/job-title bytes32
   :freelancer/hourly-rate big-num
   :freelancer/description string
   :freelancer/skills-count uint
   :freelancer/categories-count uint
   :freelancer/job-actions-count uint
   :freelancer/contracts-count uint
   :freelancer/avg-rating uint8
   :freelancer/total-earned big-num
   :freelancer/skills uint-coll
   :freelancer/categories uint-coll
   :freelancer/job-actions uint-coll
   :freelancer/contracts uint-coll
   :freelancer/ratings-count uint})

(def employer-schema
  {:employer/description string
   :employer/jobs-count uint
   :employer/avg-rating uint8
   :employer/total-paid big-num
   :employer/jobs uint-coll
   :employer/ratings-count uint})

(def member-schema
  (merge user-schema
         freelancer-schema
         employer-schema))

(def job-schema
  {:job/employer uint
   :job/title string
   :job/description string
   :job/language uint
   :job/budget big-num
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
   :job/total-paid big-num
   :job/skills uint-coll
   :job/proposals uint-coll
   :job/contracts uint-coll
   :job/invitations uint-coll})

(def job-action-schema
  {:job-action/status uint8
   :job-action/job uint
   :job-action/freelancer uint
   :proposal/rate big-num
   :proposal/created-on uint
   :proposal/description string
   :invitation/description string
   :invitation/created-on uint})

(def contract-schema
  {:contract/job uint
   :contract/freelancer uint
   :contract/rate big-num
   :contract/status uint8
   :contract/created-on uint
   :contract/total-invoiced big-num
   :contract/total-paid big-num
   :contract/freelancer-feedback string
   :contract/freelancer-feedback-rating uint8
   :contract/freelancer-feedback-on uint
   :contract/employer-feedback string
   :contract/employer-feedback-rating uint8
   :contract/employer-feedback-on uint
   :contract/done-by-freelancer? bool
   :contract/done-on uint
   :contract/invoices-count uint
   :contract/invoices uint-coll})

(def invoice-schema
  {:invoice/contract uint
   :invoice/description string
   :invoice/amount big-num
   :invoice/worked-hours uint
   :invoice/worked-from uint
   :invoice/worked-to uint
   :invoice/created-on uint
   :invoice/paid-on uint
   :invoice/cancelled-on uint
   :invoice/status uint8})

(def skill-schema
  {:skill/name bytes32
   :skill/jobs-count uint
   :skill/jobs uint-coll
   :skill/blocked? bool
   :skill/freelancers-count uint
   :skill/freelancers uint-coll})

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

(def add-invitation-args
  [:job-action/job :job-action/freelancer :invitation/description])

(def add-proposal-args
  [:job-action/job :proposal/description :proposal/rate])

(def add-contract-args
  [:contract/job :contract/rate :contract/hiring-done?])

(def add-contract-feedback-args
  [:contract/id :contract/feedback :contract/rating])

(def add-invoice-args
  [:invoice/contract :invoice/description :invoice/amount :invoice/worked-hours :invoice/worked-from
   :invoice/worked-to])

(def pay-invoice-args
  [:invoice/id])

(def cancel-invoice-args
  [:invoice/id])

(def add-skills-args
  [:skill/names])

(def get-freelancer-job-actions-args
  [:user/id :job-action/status :job/status])

(def get-freelancer-invoices-args
  [:user/id :invoice/status])

(def get-freelancer-contracts-args
  [:user/id :contract/done?])

(def get-job-contracts-args
  [:job/id])

(def get-job-proposals-args
  [:job/id])

(def get-job-invoices-args
  [:job/id :invoice/status])

(def get-employer-jobs-args
  [:user/id :job/status])

(def schema
  (merge
    user-schema
    freelancer-schema
    employer-schema
    job-schema
    job-action-schema
    contract-schema
    invoice-schema
    skill-schema))

(defn replace-big-num-types [types]
  (map #(if (= % big-num) uint %) types))

(defn create-types-map [fields types]
  (reduce
    (fn [res [i field]]
      (update res (nth types i) (comp vec conj) field))
    {} (medley/indexed fields)))

(defn parse-value [val val-type]
  (condp = val-type
    bytes32 (web3/to-ascii val)
    big-num val
    uint (.toNumber val)
    uint8 (.toNumber val)
    val))

(defn parse-entity [fields result]
  (let [types (map schema fields)
        types-map (create-types-map fields (replace-big-num-types types))]
    (reduce (fn [acc [i results-of-type]]
              (let [result-type (inc i)]
                (merge acc
                       (if (= result-type string)
                         (when-let [string-field-name (get-in types-map [result-type 0])]
                           {string-field-name results-of-type})
                         (into {}
                               (for [[j res] (medley/indexed results-of-type)]
                                 (when-let [field-name (get-in types-map [result-type j])]
                                   {field-name
                                    (parse-value res (schema field-name))})))))))
            {} (medley/indexed result))))

(defn log-entity [fields err res]
  (if err
    (console :error err)
    (console :log (parse-entity fields res))))

(defn uint->value [val val-type]
  (condp = val-type
    bool (if (= val 0) false true)
    bytes32 (web3/to-ascii (web3/from-decimal val))
    big-num val
    (.toNumber val)))

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
  (let [fields (remove #(= (schema %) uint-coll) fields)
        records (map #(u/sha3 % id) fields)
        types (map schema fields)]
    (web3-eth/contract-call instance :get-entity records (replace-big-num-types types) (partial log-entity fields))))

(defn get-entity-list [ids fields instance]
  (let [records (flatten (for [id ids]
                           (for [field fields]
                             (u/sha3 field id))))
        types (map schema fields)]
    (web3-eth/contract-call instance :get-entity-list records (replace-big-num-types types) (partial log-entities fields))))

(comment
  (create-types-map [:a :b :c] [1 2 1])
  (get-entity 1 [:user/address :user/name :user/gravatar :user/country :user/status :user/freelancer?
                 :user/employer? :user/employer? :freelancer/available? :freelancer/job-title
                 :freelancer/hourly-rate :freelancer/description :employer/description]
              (get-in @re-frame.db/app-db [:contracts :ethlance-db :instance])))