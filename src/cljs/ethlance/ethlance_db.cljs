(ns ethlance.ethlance-db
  (:require
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [cljs.spec :as s]
    [clojure.set :as set]
    [clojure.string :as string]
    [ethlance.db]
    [ethlance.utils :as u]
    [goog.date.DateTime]
    [medley.core :as medley]
    [re-frame.core :refer [console dispatch reg-fx]]))

(defn spec-form->entity-fields [spec-key & [nmsp]]
  (cond->> (last (s/form spec-key))
    nmsp (u/filter-by-namespace nmsp)
    true set))

(def user-entity-fields (set/difference (spec-form->entity-fields :app/user :user) #{:user/balance :user/id}))
(def user-balance-entity-fields #{:user/balance})
(def freelancer-entity-fields (spec-form->entity-fields :app/user :freelancer))
(def employer-entity-fields (spec-form->entity-fields :app/user :employer))

(def account-entitiy-fields
  (set/union user-entity-fields
             freelancer-entity-fields
             employer-entity-fields))

(def job-entity-fields (set/difference (spec-form->entity-fields :app/job) #{:job/id}))

(def proposal+invitation-entitiy-fields
  #{:contract/freelancer
    :contract/job
    :contract/status
    :invitation/created-on
    :invitation/description
    :proposal/created-on
    :proposal/description
    :proposal/rate})

(def employer-feedback-entity-fields
  #{:contract/employer-feedback
    :contract/employer-feedback-on
    :contract/employer-feedback-rating})

(def freelancer-feedback-entity-fields
  #{:contract/freelancer-feedback
    :contract/freelancer-feedback-on
    :contract/freelancer-feedback-rating})

(def contract-entity-fields (set/difference (spec-form->entity-fields :app/contract :contract)
                                            employer-feedback-entity-fields
                                            freelancer-feedback-entity-fields
                                            #{:contract/id}))

(def feedback-entity-fields
  (set/union employer-feedback-entity-fields
             freelancer-feedback-entity-fields
             #{:contract/freelancer
               :contract/job
               :contract/done-by-freelancer?}))

(def invoice-entity-fields (set/difference (spec-form->entity-fields :app/invoice) #{:invoice/id}))

(def invoices-table-entity-fields
  #{:invoice/contract :invoice/amount :invoice/created-on :invoice/status :invoice/paid-on})

(def skill-entity-fields (set/difference (spec-form->entity-fields :app/skill) #{:skill/id}))

(def user-editable-fields
  (set/difference (set/union account-entitiy-fields user-balance-entity-fields)
                  #{:user/address :user/created-on}))

(def job-editable-fields
  #{:job/status
    :job/contracts-count
    :job/contracts
    :job/total-paid})

(def contract-editable-fields
  #{:contract/status
    :contract/invoices-count
    :contract/invoices
    :contract/total-invoiced
    :contract/total-paid
    :contract/employer-feedback-rating
    :contract/freelancer-feedback-rating})

(def invoice-editable-fields
  #{:invoice/status})

(def wei-args
  #{:freelancer/hourly-rate :search/min-hourly-rate :search/max-hourly-rate :job/budget :search/min-budget
    :proposal/rate :invoice/amount})

(def set-user-args
  [:user/name :user/gravatar :user/country :user/state :user/languages])

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
  [:search/category :search/skills :search/min-avg-rating :search/min-freelancer-ratings-count
   :search/min-hourly-rate :search/max-hourly-rate :search/country :search/state
   :search/language :search/offset :search/limit :search/seed])

(def add-job-args
  [:job/title :job/description :job/skills :job/language :job/budget
   [:job/category :job/payment-type :job/experience-level :job/estimated-duration :job/hours-per-week
    :job/freelancers-needed]])

(def search-jobs-args
  [:search/category :search/skills :search/payment-types :search/experience-levels :search/estimated-durations
   :search/hours-per-weeks])

(def search-jobs-nested-args
  [:search/min-budget :search/min-employer-avg-rating :search/min-employer-ratings-count
   :search/country :search/state :search/language :search/offset :search/limit])

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

(def get-configs-args
  [:config/keys])

(def set-configs-args
  [:config/keys :config/values])

(def get-skill-names-args
  [:skill/offset :skill/limit])

(def block-skills-args
  [:skill/ids])

(def set-skill-name-args
  [:skill/id :skill/name])

(def set-smart-contract-status-args
  [:status])

(def eth-contracts-fns
  {:ethlance-config/add-skills add-skills-args
   :ethlance-config/block-skills block-skills-args
   :ethlance-config/get-configs get-configs-args
   :ethlance-config/set-configs set-configs-args
   :ethlance-config/set-skill-name set-skill-name-args
   :ethlance-config/set-smart-contract-status set-smart-contract-status-args
   :ethlance-contract/add-job-contract add-job-contract-args
   :ethlance-contract/add-job-contract-feedback add-job-contract-feedback-args
   :ethlance-contract/add-job-invitation add-job-invitation-args
   :ethlance-contract/add-job-proposal add-job-proposal-args
   :ethlance-contract/set-smart-contract-status set-smart-contract-status-args
   :ethlance-invoice/add-invoice add-invoice-args
   :ethlance-invoice/cancel-invoice cancel-invoice-args
   :ethlance-invoice/pay-invoice pay-invoice-args
   :ethlance-invoice/set-smart-contract-status set-smart-contract-status-args
   :ethlance-job/add-job add-job-args
   :ethlance-job/set-job-hiring-done set-job-hiring-done-args
   :ethlance-job/set-smart-contract-status set-smart-contract-status-args
   :ethlance-search/search-freelancers search-freelancers-args
   :ethlance-search/search-jobs (conj search-jobs-args search-jobs-nested-args)
   :ethlance-user/register-employer register-employer-args
   :ethlance-user/register-freelancer register-freelancer-args
   :ethlance-user/set-employer set-employer-args
   :ethlance-user/set-freelancer set-freelancer-args
   :ethlance-user/set-smart-contract-status set-smart-contract-status-args
   :ethlance-user/set-user set-user-args
   :ethlance-views/get-contract-invoices get-contract-invoices-args
   :ethlance-views/get-employer-contracts get-user-contracts-args
   :ethlance-views/get-employer-invoices get-user-invoices-args
   :ethlance-views/get-employer-jobs get-employer-jobs-args
   :ethlance-views/get-employer-jobs-for-freelancer-invite get-employer-jobs-for-freelancer-invite
   :ethlance-views/get-freelancer-contracts get-user-contracts-args
   :ethlance-views/get-freelancer-invoices get-user-invoices-args
   :ethlance-views/get-freelancers-job-contracts get-freelancers-job-contracts-args
   :ethlance-views/get-job-contracts get-job-contracts-args
   :ethlance-views/get-job-invoices get-job-invoices-args
   :ethlance-views/get-skill-count []
   :ethlance-views/get-skill-names get-skill-names-args
   :ethlance-views/get-users get-users-args})

(defn string-type-pred [field]
  (contains? #{'cljs.core/string? 'ethlance.utils/string-or-nil?} (s/form field)))

(defn no-string-types [fields]
  (set (remove string-type-pred fields)))

(defn remove-uint-coll-fields [fields]
  (remove #(= (s/form %) 'ethlance.utils/uint-coll?) fields))

(def field-pred->solidity-type
  {'cljs.core/boolean? 1
   'ethlance.utils/uint8? 2
   'ethlance.utils/uint? 3
   'ethlance.utils/address? 4
   'ethlance.utils/bytes32? 5
   'cljs.core/int? 6
   'cljs.core/string? 7
   'ethlance.utils/string-or-nil? 7
   'ethlance.utils/big-num? 3
   'ethlance.utils/big-num|num|str? 3
   'ethlance.utils/date? 3
   'ethlance.utils/date-or-nil? 3
   })

(def str-delimiter "99--DELIMITER--11")
(def list-delimiter "99--DELIMITER-LIST--11")

(defn uint->value [val field]
  (condp = (s/form field)
    'cljs.core/boolean? (if (.eq val 0) false true)
    'ethlance.utils/bytes32? (u/remove-zero-chars (web3/to-ascii (web3/from-decimal val)))
    'ethlance.utils/address? (u/prepend-address-zeros (web3/from-decimal val))
    'ethlance.utils/date? (u/big-num->date-time val)
    'ethlance.utils/date-or-nil? (u/big-num->date-time val)
    'ethlance.utils/big-num? (web3/from-wei val :ether)
    'ethlance.utils/big-num|num|str? (web3/from-wei val :ether)
    (.toNumber val)))

(defn string-blob->strings [string-blob delimiter]
  (->> (u/split-include-empty string-blob str-delimiter)
    (map (fn [s]
           (when (seq s)
             (subs s 1))))))

(defn parse-entities [ids fields result]
  (let [ids (vec ids)
        grouped-by-string (group-by string-type-pred fields)
        string-fields (get grouped-by-string true)
        uint-fields (get grouped-by-string false)
        uint-fields-count (count uint-fields)]
    (let [parsed-result
          (reduce (fn [acc [i result-item]]
                    (let [entity-index (js/Math.floor (/ i uint-fields-count))
                          field-name (nth uint-fields (mod i uint-fields-count))]
                      (assoc-in acc [(nth ids entity-index) field-name]
                                (uint->value result-item field-name))))
                  {} (medley/indexed (first result)))]
      (reduce (fn [acc [entity-index entity-strings]]
                (reduce (fn [acc [string-index string-value]]
                          (let [field-name (nth string-fields string-index)]
                            (if (seq ids)
                              (assoc-in acc [(nth ids entity-index) field-name] string-value)
                              acc)))
                        acc (medley/indexed (string-blob->strings entity-strings str-delimiter))))
              parsed-result (medley/indexed (u/split-include-empty (second result) list-delimiter))))))

(defn parse-entities-field-items [ids+sub-ids field result]
  (reduce (fn [acc [i result-item]]
            (let [[id] (nth ids+sub-ids i)]
              (update-in acc [id field] conj (uint->value result-item field))))
          {} (medley/indexed (first result))))

(defn log-entities [ids fields err res]
  (if err
    (console :error err)
    (console :log (parse-entities ids fields res))))

(defn get-entities-args [ids fields]
  (let [fields (remove-uint-coll-fields fields)
        records (flatten (for [id ids]
                           (for [field fields]
                             (u/sha3 field id))))]
    [fields records]))

(defn id-counts->ids [id-counts]
  (reduce (fn [acc [id count]]
            (concat acc (map #(vec [id %]) (range count))))
          [] id-counts))

(defn get-entities [ids fields instance on-success on-error]
  (let [[fields records] (get-entities-args ids fields)]
    (web3-eth/contract-call instance
                            :get-entity-list
                            records
                            (map (comp field-pred->solidity-type s/form) fields)
                            (fn [err result]
                              (if err
                                (on-error err)
                                (on-success (parse-entities ids fields result)))))))

(defn get-entities-field-items-args [id-counts field]
  (let [ids+sub-ids (id-counts->ids id-counts)
        records (map (fn [[id sub-id]]
                       (u/sha3 field id sub-id)) ids+sub-ids)]
    [ids+sub-ids field records [(field-pred->solidity-type 'ethlance.utils/uint?)]]))

(defn get-entities-field-items [id-counts field instance on-success on-error]
  (let [[ids+sub-ids field records types] (get-entities-field-items-args id-counts field)]
    (web3-eth/contract-call instance
                            :get-entity-list
                            records
                            types
                            (fn [err result]
                              (if err
                                (on-error on-error)
                                (on-success (parse-entities-field-items ids+sub-ids field result)))))))


(s/def ::instance (complement nil?))
(s/def ::ids (s/coll-of (s/nilable int?)))
(s/def ::fields (s/coll-of keyword?))
(s/def ::on-success sequential?)
(s/def ::on-error sequential?)

(s/def ::entities (s/keys :req-un [::instance ::ids ::fields ::on-success ::on-error]))

(reg-fx
  :ethlance-db/entities
  (fn [{:keys [:instance :ids :fields :on-success :on-error] :as config}]
    (s/assert ::entities config)
    (let [ids (->> ids
                (filter pos?)
                distinct)]
      (if (and (seq ids) (seq fields))
        (get-entities ids
                      fields
                      instance
                      #(dispatch (conj on-success %))
                      #(dispatch (conj on-error %)))
        #(dispatch (conj on-success {}))))))


(s/def ::count-key keyword?)
(s/def ::field-key keyword?)
(s/def ::items map?)
(s/def ::entities-field-items (s/keys :req-un [::instance ::items ::count-key ::field-key ::on-success ::on-error]))

(reg-fx
  :ethlance-db/entities-field-items
  (fn [{:keys [:instance :items :count-key :field-key :on-success :on-error] :as config}]
    (s/assert ::entities-field-items config)
    (get-entities-field-items (->> items
                                (medley/map-vals count-key)
                                (medley/remove-vals nil?))
                              field-key
                              instance
                              #(dispatch (conj on-success %))
                              #(dispatch (conj on-error %)))))