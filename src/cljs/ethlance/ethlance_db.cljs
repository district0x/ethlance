(ns ethlance.ethlance-db
  (:refer-clojure :exclude [int])
  (:require
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [clojure.string :as string]
    [ethlance.utils :as u]
    [medley.core :as medley]
    [re-frame.core :refer [console dispatch]]
    [clojure.set :as set]))

(def bool 1)
(def uint8 2)
(def uint 3)
(def addr 4)
(def bytes32 5)
(def int 6)
(def string 7)
(def big-num 8)
(def uint-coll 9)
(def date 10)

(def user-schema
  {:user/address addr
   :user/country uint
   :user/created-on date
   :user/employer? bool
   :user/freelancer? bool
   :user/gravatar bytes32
   :user/languages uint-coll
   :user/languages-count uint
   :user/name string
   :user/status uint8})

(def freelancer-schema
  {:freelancer/available? bool
   :freelancer/avg-rating uint8
   :freelancer/categories uint-coll
   :freelancer/categories-count uint
   :freelancer/contracts uint-coll
   :freelancer/contracts-count uint
   :freelancer/description string
   :freelancer/hourly-rate big-num
   :freelancer/job-title string
   :freelancer/ratings-count uint
   :freelancer/skills uint-coll
   :freelancer/skills-count uint
   :freelancer/total-earned big-num
   :freelancer/total-invoiced big-num})

(def employer-schema
  {:employer/avg-rating uint8
   :employer/description string
   :employer/jobs uint-coll
   :employer/jobs-count uint
   :employer/ratings-count uint
   :employer/total-paid big-num
   :employer/total-invoiced big-num
   })

(def account-schema
  (merge user-schema
         freelancer-schema
         employer-schema))

(def job-schema
  {:job/budget big-num
   :job/category uint8
   :job/contracts uint-coll
   :job/contracts-count uint
   :job/created-on date
   :job/description string
   :job/employer uint
   :job/estimated-duration uint8
   :job/experience-level uint8
   :job/freelancers-needed uint8
   :job/hiring-done-on date
   :job/hours-per-week uint8
   :job/language uint
   :job/payment-type uint8
   :job/skills uint-coll
   :job/skills-count uint
   :job/status uint8
   :job/title string
   :job/total-paid big-num})

(def proposal+invitation-schema
  {:contract/freelancer uint
   :contract/job uint
   :contract/status uint8
   :invitation/created-on date
   :invitation/description string
   :proposal/created-on date
   :proposal/description string
   :proposal/rate big-num})

(def contract-schema
  {:contract/created-on date
   :contract/description string
   :contract/done-by-freelancer? bool
   :contract/done-on date
   :contract/freelancer uint
   :contract/invoices uint-coll
   :contract/invoices-count uint
   :contract/job uint
   :contract/status uint8
   :contract/total-invoiced big-num
   :contract/total-paid big-num})

(def employer-feedback-schema
  {:contract/employer-feedback string
   :contract/employer-feedback-on date
   :contract/employer-feedback-rating uint8})

(def freelancer-feedback-schema
  {:contract/freelancer-feedback string
   :contract/freelancer-feedback-on date
   :contract/freelancer-feedback-rating uint8})

(def feedback-schema
  (merge employer-feedback-schema
         freelancer-feedback-schema
         (select-keys contract-schema
                      [:contract/freelancer
                       :contract/job
                       :contract/done-by-freelancer?])))

(def contract-all-schema
  (merge proposal+invitation-schema
         contract-schema
         feedback-schema))

(def invoice-schema
  {:invoice/amount big-num
   :invoice/cancelled-on date
   :invoice/contract uint
   :invoice/created-on date
   :invoice/description string
   :invoice/paid-on date
   :invoice/status uint8
   :invoice/worked-from date
   :invoice/worked-hours uint
   :invoice/worked-to date})

(def invoices-table-schema
  (select-keys invoice-schema [:invoice/contract :invoice/amount :invoice/created-on :invoice/status :invoice/paid-on]))

(def skill-schema
  {:skill/name bytes32
   :skill/creator uint
   :skill/created-on date
   :skill/jobs-count uint
   :skill/jobs uint-coll
   :skill/blocked? bool
   :skill/freelancers-count uint
   :skill/freelancers uint-coll})

(def user-editable-fields
  (set/difference (set (keys account-schema)) #{:user/address :user/created-on}))

(def job-editable-fields
  #{:job/status
    :job/contracts-count
    :job/contracts
    :job/total-paid
    :job/hiring-done-on})

(def contract-editable-fields
  #{:contract/status
    :contract/invoices-count
    :contract/invoices
    :contract/total-invoiced
    :contract/total-paid
    :contract/done-on
    :proposal/created-on
    :invitation/created-on
    :contract/created-on
    :contract/freelancer-feedback-on
    :contract/employer-feedback-on})

(def invoice-editable-fields
  #{:invoice/status :invoice/paid-on :invoice/cancelled-on})


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
  [:search/category :search/skills :search/min-avg-rating :search/min-freelancer-ratings-count :search/min-hourly-rate
   :search/max-hourly-rate :search/country :search/language :search/offset :search/limit])

(def add-job-args
  [:job/title :job/description :job/skills :job/language :job/budget
   [:job/category :job/payment-type :job/experience-level :job/estimated-duration :job/hours-per-week
    :job/freelancers-needed]])

(def search-jobs-args
  [:search/category :search/skills :search/payment-types :search/experience-levels :search/estimated-durations
   :search/hours-per-weeks])

(def search-jobs-nested-args
  [:search/min-budget :search/min-employer-avg-rating :search/min-employer-ratings-count
   :search/country :search/language :search/offset :search/limit])

(def set-job-hiring-done-args
  [:job/id])

(def add-job-invitation-args
  [:contract/job :contract/freelancer :invitation/description])

(def add-job-proposal-args
  [:contract/job :proposal/description :proposal/rate])

(def add-job-contract-args
  [:contract/id :contract/description :contract/hiring-done?])

(def add-job-contract-feedback-args
  [:contract/id :contract/feedback :contract/feedback-rating])

(def add-invoice-args
  [:invoice/contract :invoice/description :invoice/amount :invoice/worked-hours :invoice/worked-from
   :invoice/worked-to])

(def pay-invoice-args
  [:invoice/id])

(def cancel-invoice-args
  [:invoice/id])

(def add-skills-args
  [:skill/names])

(def get-user-invoices-args
  [:user/id :invoice/status])

(def get-user-contracts-args
  [:user/id :contract/status :job/status])

(def get-job-contracts-args
  [:job/id :contract/status])

(def get-job-invoices-args
  [:job/id :invoice/status])

(def get-contract-invoices-args
  [:contract/id :invoice/status])

(def get-freelancers-job-contracts-args
  [:user/ids :job/id])

(def get-employer-jobs-args
  [:user/id :job/status])

(def get-users-args
  [:user/addresses])

(def get-employer-jobs-for-freelancer-invite
  [:employer/id :freelancer/id])

(def eth-contracts-fns
  {:ethlance-views/get-freelancer-contracts get-user-contracts-args
   :ethlance-views/get-employer-contracts get-user-contracts-args
   :ethlance-views/get-freelancer-invoices get-user-invoices-args
   :ethlance-views/get-employer-invoices get-user-invoices-args
   :ethlance-views/get-job-contracts get-job-contracts-args
   :ethlance-views/get-job-invoices get-job-invoices-args
   :ethlance-views/get-freelancers-job-contracts get-freelancers-job-contracts-args
   :ethlance-views/get-contract-invoices get-contract-invoices-args
   :ethlance-views/get-employer-jobs get-employer-jobs-args
   :ethlance-views/get-skill-names #{}
   :ethlance-views/get-users get-users-args
   :ethlance-views/get-employer-jobs-for-freelancer-invite get-employer-jobs-for-freelancer-invite
   :ethlance-search/search-freelancers search-freelancers-args
   :ethlance-search/search-jobs (conj search-jobs-args search-jobs-nested-args)
   :ethlance-config/add-skills add-skills-args
   :ethlance-invoice/cancel-invoice cancel-invoice-args
   :ethlance-invoice/pay-invoice pay-invoice-args
   :ethlance-invoice/add-invoice add-invoice-args
   :ethlance-contract/add-job-invitation add-job-invitation-args
   :ethlance-contract/add-job-contract add-job-contract-args
   :ethlance-contract/add-job-proposal add-job-proposal-args
   :ethlance-contract/add-job-contract-feedback add-job-contract-feedback-args
   :ethlance-job/add-job add-job-args
   :ethlance-job/set-job-hiring-done set-job-hiring-done-args
   :ethlance-user/register-freelancer register-freelancer-args
   :ethlance-user/register-employer register-employer-args
   :ethlance-user/set-freelancer set-freelancer-args
   :ethlance-user/set-employer set-employer-args
   :ethlance-user/set-user set-user-args
   })

(def schema
  (merge
    contract-schema
    employer-schema
    feedback-schema
    freelancer-schema
    invoice-schema
    job-schema
    proposal+invitation-schema
    skill-schema
    user-schema))

(defn without-strs [schema]
  (medley/remove-vals (partial = string) schema))

(defn remove-uint-coll-fields [fields]
  (remove #(= (schema %) uint-coll) fields))

(defn replace-special-types [types]
  (map #(if (or (= % date) (= % big-num)) uint %) types))

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

(def str-delimiter "99--DELIMITER--11")
(def list-delimiter "99--DELIMITER-LIST--11")

(defn parse-entity [fields result]
  (let [types (map schema fields)
        types-map (create-types-map fields (replace-special-types types))]
    (reduce (fn [acc [i results-of-type]]
              (let [result-type (inc i)
                    results-of-type (if (= result-type string)
                                      (string/split results-of-type str-delimiter)
                                      results-of-type)]
                (merge acc
                       (into {}
                             (for [[j res] (medley/indexed results-of-type)]
                               (when-let [field-name (get-in types-map [result-type j])]
                                 {field-name
                                  (parse-value res (schema field-name))}))))))
            {} (medley/indexed result))))

(defn log-entity [fields err res]
  (if err
    (console :error err)
    (console :log (parse-entity fields res))))

(defn uint->value [val val-type]
  (condp = val-type
    bool (if (.eq val 0) false true)
    bytes32 (web3/to-ascii (web3/from-decimal val))
    addr (web3/from-decimal val)
    date (u/big-num->date-time val)
    big-num val
    (.toNumber val)))

(defn parse-entities [ids fields result]
  (let [ids (vec ids)
        uint-fields (remove #(= string (schema %)) fields)
        string-fields (filter #(= string (schema %)) fields)
        uint-fields-count (count uint-fields)]
    (let [parsed-result
          (reduce (fn [acc [i result-item]]
                    (let [entity-index (js/Math.floor (/ i uint-fields-count))
                          field-name (nth uint-fields (mod i uint-fields-count))]
                      (assoc-in acc [(nth ids entity-index) field-name]
                                (uint->value result-item (schema field-name)))))
                  {} (medley/indexed (first result)))]
      (reduce (fn [acc [entity-index entity-strings]]
                (reduce (fn [acc [string-index string-value]]
                          (let [field-name (nth string-fields string-index)]
                            (if (seq ids)
                              (assoc-in acc [(nth ids entity-index) field-name] string-value)
                              acc)))
                        acc (medley/indexed (string/split entity-strings str-delimiter))))
              parsed-result (medley/indexed (string/split (second result) list-delimiter))))))

(defn parse-entities-field-items [ids+sub-ids field result]
  (reduce (fn [acc [i result-item]]
            (let [[id] (nth ids+sub-ids i)]
              (update-in acc [id field] conj (uint->value result-item uint))))
          {} (medley/indexed (first result))))

(defn log-entities [ids fields err res]
  (if err
    (console :error err)
    (console :log (parse-entities ids fields res))))

(defn get-entity-args [id fields]
  (let [fields (remove-uint-coll-fields fields)
        records (map #(u/sha3 % id) fields)
        types (replace-special-types (map schema fields))]
    [fields records types]))

(defn get-entity [id fields instance]
  (let [[fields records types] (get-entity-args id fields)]
    (web3-eth/contract-call instance :get-entity records (replace-special-types types) (partial log-entity fields))))

(defn get-entities-args [ids fields]
  (let [fields (remove-uint-coll-fields fields)
        records (flatten (for [id (set ids)]
                           (for [field fields]
                             (u/sha3 field id))))
        types (replace-special-types (map schema fields))]
    [fields records types]))

(defn id-counts->ids [id-counts]
  (reduce (fn [acc [id count]]
            (concat acc (map #(vec [id %]) (range count))))
          [] id-counts))

(defn get-entities-field-items-args [id-counts field]
  (let [ids+sub-ids (id-counts->ids id-counts)
        records (map (fn [[id sub-id]]
                       (u/sha3 field id sub-id)) ids+sub-ids)]
    [ids+sub-ids field records [uint]]))

(defn get-entities [ids fields instance]
  (let [[fields records types] (get-entities-args ids fields)]
    (web3-eth/contract-call instance :get-entity-list records
                            (replace-special-types types)
                            (partial log-entities ids fields))))

(defn get-entities-field-items [id-counts field instance]
  (let [[ids+sub-ids field records types] (get-entities-field-items-args id-counts field)]
    (web3-eth/contract-call instance :get-entity-list records types
                            (fn [err result]
                              (when err
                                (console :error err))
                              (console :log (parse-entities-field-items ids+sub-ids field result))))))

(comment
  (create-types-map [:a :b :c] [1 2 1])
  (get-entity 1 (keys account-schema)
              (get-in @re-frame.db/app-db [:eth/contracts :ethlance-db :instance]))
  (get-entities [1] (keys account-schema) (get-in @re-frame.db/app-db [:eth/contracts :ethlance-db :instance]))
  (get-entities-field-items {1 8} :freelancer/skills
                            (get-in @re-frame.db/app-db [:eth/contracts :ethlance-db :instance]))
  (id-counts->ids {1 0}))