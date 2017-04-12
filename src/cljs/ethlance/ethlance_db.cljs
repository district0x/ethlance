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

(def user-entity-fields (set/difference (spec-form->entity-fields :app/user :user)
                                        #{:user/balance :user/id :user/email :user/sponsorships
                                          :user/sponsorships-count :user/address}))
(def user-balance-entity-fields #{:user/balance})
(def user-notifications-fields (spec-form->entity-fields :app/user :user.notif))
(def freelancer-entity-fields (set/difference (spec-form->entity-fields :app/user :freelancer)))
(def employer-entity-fields (spec-form->entity-fields :app/user :employer))

(def account-entitiy-fields
  (set/union user-entity-fields
             freelancer-entity-fields
             employer-entity-fields
             user-notifications-fields))

(def job-entity-fields (set/difference (spec-form->entity-fields :app/job) #{:job/id}))

(def job-sponsorship-stats-fields
  #{:job/sponsorships-total
    :job/sponsorships-balance
    :job/sponsorships-total-refunded})

(def proposal-entity-fields
  #{:proposal/created-on
    :proposal/description
    :proposal/rate})

(def invitation-entity-fields
  #{:invitation/created-on
    :invitation/description})

(def proposal+invitation-entitiy-fields
  (set/union
    #{:contract/freelancer
      :contract/job
      :contract/status}
    invitation-entity-fields
    proposal-entity-fields))

(def job-proposals-list-fields
  #{:contract/freelancer
    :proposal/rate
    :proposal/created-on
    :invitation/created-on
    :contract/status
    :contract/job
    :user/name})

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
               :contract/done-by-freelancer?
               :contract/cancelled-on
               :contract/cancel-description}))

(def feedback-list-fields
  (set/union feedback-entity-fields
             #{:job/employer
               :user/name
               :user/gravatar}))

(def invoice-entity-fields (set/difference (spec-form->entity-fields :app/invoice) #{:invoice/id}))

(def invoices-list-fields
  #{:invoice/contract :invoice/amount :invoice/created-on :invoice/status :invoice/paid-on})

(def skill-entity-fields (set/difference (spec-form->entity-fields :app/skill) #{:skill/id}))
(def message-entity-fields (set/difference (spec-form->entity-fields :app/message :message) #{:message/id}))

(def sponsorship-entity-fields (set/difference (spec-form->entity-fields :app/sponsorship) #{:sponsorship/id}))

(def job-sponsorships-table-entity-fields
  #{:sponsorship/amount :sponsorship/name :sponsorship/link :sponsorship/refunded? :sponsorship/updated-on
    :sponsorship/refunded-amount :sponsorship/user})

(def search-jobs-fields
  #{:job/title :job/payment-type :job/estimated-duration :job/experience-level :job/hours-per-week
    :job/created-on :job/budget :job/skills :job/skills-count :job/employer :job/reference-currency
    :job/category :job/sponsorable? :employer/jobs-count :employer/avg-rating :employer/total-paid :user/name
    :employer/ratings-count :user/country :user/state :user/balance})

(def get-sponsorable-jobs-fields
  #{:job/title :job/created-on :job/skills :job/skills-count :job/employer :job/category :job/sponsorable?
    :job/sponsorships-balance :job/sponsorships-count :job/sponsorships-total :employer/jobs-count
    :job/status :employer/avg-rating :employer/total-paid :user/name :employer/ratings-count :user/country
    :user/state})

(def search-freelancers-fields
  #{:freelancer/avg-rating :freelancer/hourly-rate :freelancer/hourly-rate-currency :freelancer/job-title
    :freelancer/ratings-count :freelancer/skills :freelancer/skills-count :user/name :user/gravatar
    :user/country :user/state :user/freelancer? :user/employer? :freelancer/available?})

(def user-editable-fields
  (set/difference (set/union account-entitiy-fields user-balance-entity-fields #{:user/email})
                  #{:user/created-on}))

#_(def job-editable-fields
    #{:job/status
      :job/contracts-count
      :job/contracts
      :job/total-paid
      :job/sponsorships-balance
      :job/sponsorships-total
      :job/sponsorships-total-refunded})

(def job-editable-fields (set/difference job-entity-fields #{:job/created-on}))

(def contract-editable-fields
  #{:contract/status
    :contract/invoices-count
    :contract/invoices
    :contract/total-invoiced
    :contract/total-paid
    :contract/employer-feedback-rating
    :contract/freelancer-feedback-rating
    :contract/messages
    :contract/messages-count
    :contract/job})

(def invoice-editable-fields
  #{:invoice/status :invoice/paid-by :invoice/contract})

(def message-editable-fields
  #{})

(def sponsorship-editable-fields
  #{:sponsorship/amount :sponsorship/name :sponsorship/link :sponsorship/updated-on :sponsorship/refunded?
    :sponsorship/refunded-amount})

(def wei-args
  #{:freelancer/hourly-rate :search/min-hourly-rates :search/max-hourly-rates :job/budget :search/min-budgets
    :proposal/rate :invoice/amount :invoice/rate :invoice/conversion-rate})

(def set-user-args
  [:user/name :user/email :user/gravatar :user/country :user/state :user/languages :user/github :user/linkedin])

(def set-freelancer-args
  [:freelancer/available? :freelancer/job-title :freelancer/hourly-rate :freelancer/hourly-rate-currency
   :freelancer/categories :freelancer/skills :freelancer/description])

(def register-freelancer-args
  (concat set-user-args set-freelancer-args))

(def set-employer-args
  [:employer/description])

(def register-employer-args
  (concat set-user-args set-employer-args))

(def set-user-notifications-args
  [[:user.notif/disabled-all? :user.notif/disabled-newsletter? :user.notif/disabled-on-job-invitation-added?
    :user.notif/disabled-on-job-contract-added? :user.notif/disabled-on-invoice-paid?
    :user.notif/disabled-on-job-proposal-added? :user.notif/disabled-on-invoice-added?
    :user.notif/disabled-on-job-contract-feedback-added? :user.notif/disabled-on-message-added?
    :user.notif/disabled-on-job-sponsorship-added?]
   [:user.notif/job-recommendations]])

(def search-freelancers-args
  [:search/category :search/skills :search/skills-or :search/min-avg-rating :search/min-freelancer-ratings-count
   :search/min-hourly-rates :search/max-hourly-rates])

(def search-freelancers-nested-args
  [:search/country :search/state :search/language :search/job-recommendations :search/offset :search/limit :search/seed])

(def set-job-args
  [:job/id :job/title :job/description :job/skills :job/language :job/budget
   [:job/category :job/payment-type :job/experience-level :job/estimated-duration :job/hours-per-week
    :job/freelancers-needed :job/reference-currency]
   :job/sponsorable? :job/allowed-users])

(def search-jobs-args
  [:search/category :search/skills :search/skills-or :search/payment-types :search/experience-levels
   :search/estimated-durations :search/hours-per-weeks :search/min-budgets])

(def search-jobs-nested-args
  [:search/min-employer-avg-rating :search/min-employer-ratings-count
   :search/country :search/state :search/language :search/min-created-on :search/offset :search/limit])

(def get-sponsorable-jobs-args
  [])

(def set-job-hiring-done-args
  [:job/id])

(def approve-sponsorable-job-args
  [:job/id])

(def add-job-invitation-args
  [:contract/job :contract/freelancer :invitation/description])

(def add-job-proposal-args
  [:contract/job :proposal/description :proposal/rate])

(def add-job-contract-args
  [:contract/id :contract/description :contract/hiring-done?])

(def cancel-job-contract-args
  [:contract/id :contract/cancel-description])

(def add-job-contract-feedback-args
  [:contract/id :contract/feedback :contract/feedback-rating])

(def add-invoice-args
  [:invoice/contract :invoice/description])

(def add-invoice-nested-args
  [:invoice/rate :invoice/conversion-rate :invoice/worked-hours :invoice/worked-minutes :invoice/worked-from
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
  [:user/id :contract/statuses :job/statuses])

(def get-job-contracts-args
  [:job/id :contract/status])

(def get-job-invoices-args
  [:job/id :invoice/status])

(def get-contract-invoices-args
  [:contract/id :invoice/status])

(def get-contract-messages-args
  [:contract/id])

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

(def add-job-contract-message-args
  [:contract/id :message/text])

(def get-job-sponsorships-args
  [:job/id])

(def get-user-sponsorships-args
  [:user/id])

(def get-job-approvals-args
  [:job/id])

(def add-job-sponsorship-args
  [:sponsorship/job :sponsorship/name :sponsorship/link])

(def refund-job-sponsorship-args
  [:sponsorship/job :limit])

(def eth-contracts-fns
  {:ethlance-config/add-skills add-skills-args
   :ethlance-config/block-skills block-skills-args
   :ethlance-config/get-configs get-configs-args
   :ethlance-config/set-configs set-configs-args
   :ethlance-config/set-skill-name set-skill-name-args
   :ethlance-config/set-smart-contract-status set-smart-contract-status-args
   :ethlance-contract/add-job-contract add-job-contract-args
   :ethlance-contract/add-job-invitation add-job-invitation-args
   :ethlance-contract/add-job-proposal add-job-proposal-args
   :ethlance-contract/cancel-job-contract cancel-job-contract-args
   :ethlance-contract/set-smart-contract-status set-smart-contract-status-args
   :ethlance-feedback/add-job-contract-feedback add-job-contract-feedback-args
   :ethlance-invoice/add-invoice (conj add-invoice-args add-invoice-nested-args)
   :ethlance-invoice/cancel-invoice cancel-invoice-args
   :ethlance-invoice/pay-invoice pay-invoice-args
   :ethlance-invoice/set-smart-contract-status set-smart-contract-status-args
   :ethlance-job/approve-sponsorable-job approve-sponsorable-job-args
   :ethlance-job/set-job set-job-args
   :ethlance-job/set-job-hiring-done set-job-hiring-done-args
   :ethlance-job/set-smart-contract-status set-smart-contract-status-args
   :ethlance-message/add-job-contract-message add-job-contract-message-args
   :ethlance-search-freelancers/search-freelancers (conj search-freelancers-args search-freelancers-nested-args)
   :ethlance-search-jobs/search-jobs (conj search-jobs-args search-jobs-nested-args)
   :ethlance-sponsor/add-job-sponsorship add-job-sponsorship-args
   :ethlance-sponsor/refund-job-sponsorships refund-job-sponsorship-args
   :ethlance-user/register-employer register-employer-args
   :ethlance-user/register-freelancer register-freelancer-args
   :ethlance-user/set-employer set-employer-args
   :ethlance-user/set-freelancer set-freelancer-args
   :ethlance-user/set-smart-contract-status set-smart-contract-status-args
   :ethlance-user/set-user set-user-args
   :ethlance-user2/set-user-notifications set-user-notifications-args
   :ethlance-views/get-contract-invoices get-contract-invoices-args
   :ethlance-views/get-contract-messages get-contract-messages-args
   :ethlance-views/get-employer-contracts get-user-contracts-args
   :ethlance-views/get-employer-invoices get-user-invoices-args
   :ethlance-views/get-employer-jobs get-employer-jobs-args
   :ethlance-views/get-employer-jobs-for-freelancer-invite get-employer-jobs-for-freelancer-invite
   :ethlance-views/get-freelancer-contracts get-user-contracts-args
   :ethlance-views/get-freelancer-invoices get-user-invoices-args
   :ethlance-views/get-freelancers-job-contracts get-freelancers-job-contracts-args
   :ethlance-views/get-job-approvals get-job-approvals-args
   :ethlance-views/get-job-contracts get-job-contracts-args
   :ethlance-views/get-job-invoices get-job-invoices-args
   :ethlance-views/get-job-sponsorships get-job-sponsorships-args
   :ethlance-views/get-skill-count []
   :ethlance-views/get-skill-names get-skill-names-args
   :ethlance-views/get-sponsorable-jobs get-sponsorable-jobs-args
   :ethlance-views/get-user-sponsorships get-user-sponsorships-args
   :ethlance-views/get-users get-users-args})

(defn string-type? [field]
  (contains? #{'cljs.core/string? 'ethlance.utils/string-or-nil?} (s/form field)))

(defn no-string-types [fields]
  (set (remove string-type? fields)))

(defn remove-coll-fields [fields]
  (remove #(contains? #{'ethlance.utils/uint-coll? 'ethlance.utils/address-coll?} (s/form %)) fields))

(defn estimate-form-data-gas [form-data]
  (reduce (fn [acc [k v]]
            (if (and (s/get-spec k) (string-type? k))
              (+ acc (u/estimate-string-gas v))
              acc))
          0 form-data))

(def field-pred->solidity-type
  {'cljs.core/boolean? 1
   'ethlance.utils/uint8? 2
   'ethlance.utils/uint? 3
   'ethlance.utils/uint-coll? 3
   'ethlance.utils/uint-or-nil? 3
   'ethlance.utils/address? 4
   'ethlance.utils/address-coll? 4
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
    'ethlance.utils/address-coll? (u/prepend-address-zeros (web3/from-decimal val))
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
        grouped-by-string (group-by string-type? fields)
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
  (let [fields (remove-coll-fields fields)
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
    [ids+sub-ids field records [(field-pred->solidity-type (s/form field))]]))

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
(s/def ::ids (s/or :int-ids (s/coll-of (s/nilable int?))
                   :address-ids (s/coll-of (s/nilable u/address?))))
(s/def ::fields (s/coll-of keyword?))
(s/def ::on-success sequential?)
(s/def ::on-error sequential?)
(s/def ::partitions (s/and int? pos?))

(s/def ::entities (s/keys :req-un [::instance ::ids ::fields ::on-success ::on-error]
                          :opt-un [::partitions]))

(reg-fx
  :ethlance-db/entities
  (fn [{:keys [:instance :ids :fields :on-success :on-error :partitions] :as config}]
    (s/assert ::entities config)
    (let [ids (->> ids
                (filter pos?)
                distinct)]
      (if (and (seq ids) (seq fields))
        (doseq [part-ids (partition-all (or partitions (count ids)) ids)]
          (get-entities part-ids
                        fields
                        instance
                        #(dispatch (conj on-success %))
                        #(dispatch (conj on-error %))))
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