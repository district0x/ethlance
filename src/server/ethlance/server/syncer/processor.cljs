(ns ethlance.server.syncer.processor
  "Processes events that have been passed through the "
  (:require
   [cuerdas.core :as str]
   [clojure.pprint :refer [pprint]]
   [bignumber.core :as bn]
   [ethlance.server.db :as db]
   [taoensso.timbre :as log]
   [district.server.config :refer [config]]
   [district.shared.error-handling :refer [try-catch]]
   [district.server.web3 :refer [web3]]
   [clojure.core.async :as async :refer [go go-loop <! >! chan] :include-macros true]

   ;; Enums
   [ethlance.shared.enum.currency-type :as enum.currency]
   [ethlance.shared.enum.payment-type :as enum.payment]
   [ethlance.shared.enum.bid-option :as enum.bid-option]
   [ethlance.shared.enum.comment-type :as enum.comment-type]
   [ethlance.shared.enum.user-type :as enum.user-type]

   ;; Ethlance Models
   [ethlance.server.model.job :as model.job]
   [ethlance.server.model.user :as model.user]
   [ethlance.server.model.arbiter :as model.arbiter]
   [ethlance.server.model.candidate :as model.candidate]
   [ethlance.server.model.employer :as model.employer]
   [ethlance.server.model.comment :as model.comment]
   [ethlance.server.model.feedback :as model.feedback]
   
   ;; Ethlance Contracts
   [ethlance.server.contract.ethlance-user :as contract.user :include-macros true]
   [ethlance.server.contract.ethlance-user-factory :as contract.user-factory]
   [ethlance.server.contract.ethlance-job-store :as contract.job :include-macros true]
   [ethlance.server.contract.ethlance-job-factory :as contract.job-factory]
   [ethlance.server.contract.ethlance-work-contract :as contract.work-contract :include-macros true]
   [ethlance.server.contract.ethlance-invoice :as contract.invoice :include-macros true]
   [ethlance.server.contract.ethlance-dispute :as contract.dispute :include-macros true]
   [ethlance.server.contract.ethlance-comment :as contract.comment :include-macros true]
   [ethlance.server.contract.ethlance-feedback :as contract.feedback :include-macros true]
   [ethlance.server.contract.ethlance-registry :as contract.registry]

   ;; Misc.
   [ethlance.server.ipfs :as ipfs]
   [ethlance.shared.async-utils :refer [<!-<log <!-<throw flush! go-try] :include-macros true]))


(defn pp-str [x]
  (with-out-str (pprint x)))


(defmulti process-event
  "Process an emitted event based on the `event-multiplexer/event-watchers` key.

  # Notes

  - Implementations are expected to return a single value async channel."
  :name)


(defmethod process-event :default
  [{:keys [name args] :as event}]
  (go (log/warn (str/format "Unprocessed Event: %s\n%s" (pr-str name) (pp-str event)))))


(declare process-registry-event)
(defmethod process-event :registry-event
  [event]
  (go (<! (process-registry-event event))))


(defmulti process-registry-event
  "Process a :registry-event. Each registry event has a unique :event_name

  # Notes

  - Similar to `process-event`, implementations must return a channel
  which places a value on completion.

  - The event name is a kebab-cased keyword from the
  original :event_name

    ex. UserRegistered --> :user-registered
  "
  (fn [{:keys [args]}] (-> args :event_name str/keyword)))


(defmethod process-registry-event :default
  [{:keys [args] :as event}]
  (go (log/warn (str/format "Unprocessed Registry Event: %s\n%s"
                            (-> args :event_name str/keyword pr-str)
                            (pp-str event)))))


(defmethod process-registry-event :user-registered
  [{:keys [args]}]
  (go-try
   (let [user-id (-> args :event_data first bn/number)
         uaddress (<!-<throw (contract.user-factory/user-by-id user-id))
         metahash-ipfs (<!-<throw (contract.user/metahash-ipfs uaddress))
         ipfs-data (<!-<throw (ipfs/get-edn metahash-ipfs))
         user-address (<!-<throw (contract.user/user-address uaddress))
         date-created (<!-<throw (contract.user/date-created uaddress))
         date-updated (<!-<throw (contract.user/date-updated uaddress))
         
         user-data (assoc ipfs-data
                          :user/id user-id
                          :user/address user-address
                          :user/date-updated (bn/number date-updated)
                          :user/date-created (bn/number date-created))]
     (model.user/register! user-data)
     
     ;; Update User Language Listing
     (model.user/update-language-listing! user-id (:user/languages ipfs-data)))))


(defmethod process-registry-event :user-registered-employer
  [{:keys [args]}]
  (go-try
   (let [user-id (-> args :event_data first bn/number)
         timestamp (-> args :timestamp bn/number)
         uaddress (<!-<throw (contract.user-factory/user-by-id user-id))
         metahash-ipfs (<!-<throw (contract.user/metahash-ipfs uaddress))
         ipfs-data (<!-<throw (ipfs/get-edn metahash-ipfs))
         date-updated (<!-<throw (contract.user/date-updated uaddress))
         user-data {:user/id user-id :user/date-updated (bn/number date-updated)}
         employer-data (assoc ipfs-data
                              :user/id user-id
                              :employer/date-registered timestamp)]
     (model.user/update! user-data)
     (model.employer/register! employer-data))))


(defmethod process-registry-event :user-registered-candidate
  [{:keys [args]}]
  (go-try
   (let [user-id (-> args :event_data first bn/number)
         timestamp (-> args :timestamp bn/number)
         uaddress (<!-<throw (contract.user-factory/user-by-id user-id))
         metahash-ipfs (<!-<throw (contract.user/metahash-ipfs uaddress))
         ipfs-data (<!-<throw (ipfs/get-edn metahash-ipfs))
         date-updated (<!-<throw (contract.user/date-updated uaddress))
         user-data {:user/id user-id :user/date-updated (bn/number date-updated)}
         candidate-data (assoc ipfs-data
                               :user/id user-id
                               :candidate/date-registered timestamp)]
     (model.user/update! user-data)
     (model.candidate/register! candidate-data)
     
     ;; update candidate categories
     (model.candidate/update-category-listing! user-id (or (:candidate/categories ipfs-data) []))
     
     ;; update skills
     (model.candidate/update-skill-listing! user-id (or (:candidate/skills ipfs-data) [])))))


(defmethod process-registry-event :user-registered-arbiter
  [{:keys [args]}]
  (go-try
   (let [user-id (-> args :event_data first bn/number)
         timestamp (-> args :timestamp bn/number)
         uaddress (<!-<throw (contract.user-factory/user-by-id user-id))

         {:keys [payment-value currency-type payment-type]} (<!-<throw (contract.user/arbiter-data uaddress))
         metahash-ipfs (<!-<throw (contract.user/metahash-ipfs uaddress))
         ipfs-data (<!-<throw (ipfs/get-edn metahash-ipfs))
         date-updated (<!-<throw (contract.user/date-updated uaddress))
         user-data {:user/id user-id :user/date-updated (bn/number date-updated)}
         arbiter-data (assoc ipfs-data
                             :user/id user-id
                             :arbiter/date-registered timestamp
                             :arbiter/currency-type currency-type
                             :arbiter/payment-value (bn/number payment-value)
                             :arbiter/payment-type payment-type)]
     (model.user/update! user-data)
     (model.arbiter/register! arbiter-data))))


(defmethod process-registry-event :job-store-created
  [{:keys [args]}]
  (go-try
   (let [job-index (-> args :event_data first bn/number)
         timestamp (-> args :timestamp bn/number)
         job-address (<!-<throw (contract.job-factory/job-store-by-index job-index))
         metahash-ipfs (<!-<throw (contract.job/metahash job-address))
         ipfs-data (<!-<throw (ipfs/get-edn metahash-ipfs))
         bid-option (<!-<throw  (contract.job/bid-option job-address))
         date-created (bn/number (<!-<throw (contract.job/date-created job-address)))
         date-updated (bn/number (<!-<throw (contract.job/date-updated job-address)))
         date-finished (bn/number (<!-<throw (contract.job/date-finished job-address)))
         employer-address (<!-<throw (contract.job/employer-address job-address))
         estimated-length-seconds (bn/number (<!-<throw (contract.job/estimated-length-seconds job-address)))
         include-ether-token? (<!-<throw (contract.job/include-ether-token? job-address))
         is-invitation-only? (<!-<throw (contract.job/is-invitation-only? job-address))

         job-data (assoc ipfs-data
                         :job/index job-index
                         :job/bid-option bid-option
                         :job/date-created date-created
                         :job/date-updated date-updated
                         :job/date-finshed date-finished
                         :job/employer-uid employer-address
                         :job/estimated-length-seconds estimated-length-seconds
                         :job/include-ether-token? include-ether-token?
                         :job/is-invitation-only? is-invitation-only?)]
     (model.job/create-job! job-data)
     (model.job/update-skill-listing! job-index (get ipfs-data :job/skills [])))))


(defmethod process-registry-event :job-arbiter-requested
  [{:keys [args]}]
  (go-try
   (let [job-index (-> args :event_data first bn/number)
         user-id (-> args :event_data second bn/number)
         timestamp (-> args :timestamp bn/number)
         job-address (<!-<throw (contract.job-factory/job-store-by-index job-index)) 
         date-updated (contract.job/date-updated job-address)
         arbiter-request-index (dec (<!-<throw (contract.job/requested-arbiter-count job-address)))
         {:keys [is-employer-request? date-requested arbiter-address]}
         (<!-<throw (contract.job/requested-arbiter-by-index job-address arbiter-request-index))
         arbiter-data {:job/index job-index
                       :user/id user-id
                       :arbiter-request/date-requested (bn/number date-requested)
                       :arbiter-request/is-employer-request? is-employer-request?}]
     (model.job/update-job! {:job/index job-index :job/date-updated date-updated})
     (model.job/add-arbiter-request! arbiter-data))))


(defmethod process-registry-event :job-arbiter-accepted
  [{:keys [args]}]
  (go-try
   (let [job-index (-> args :event_data first bn/number)
         user-id (-> args :event_data second bn/number)
         timestamp (-> args :timestamp bn/number)
         job-address (<!-<throw (contract.job-factory/job-store-by-index job-index))
         date-updated (<!-<throw (contract.job/date-updated job-address))
         accepted-arbiter (<!-<throw (contract.job/accepted-arbiter job-address))]
     (model.job/update-job! {:job/index job-index
                             :job/accepted-arbiter accepted-arbiter
                             :job/date-updated date-updated}))))


(defmethod process-registry-event :job-request-work-contract
  [{:keys [args] :as event}]
  (go-try
   (let [job-index (-> args :event_data first bn/number)
         work-index (-> args :event_data second bn/number)
         timestamp (-> args :timestamp bn/number)
         job-address (<!-<throw (contract.job-factory/job-store-by-index job-index))
         work-address (<!-<throw (contract.job/work-contract-by-index job-address work-index))
         contract-status (<!-<throw (contract.work-contract/contract-status work-address))
         candidate-address (<!-<throw (contract.work-contract/candidate-address work-address))
         date-updated (<!-<throw (contract.work-contract/date-updated work-address))
         date-created (<!-<throw (contract.work-contract/date-created work-address))
         work-contract-data {:job/index job-index
                             :work-contract/index work-index
                             :work-contract/contract-status contract-status
                             :work-contract/candidate-address candidate-address
                             :work-contract/date-updated (bn/number date-updated)
                             :work-contract/date-created (bn/number date-created)}]
     (model.job/create-work-contract! work-contract-data))))


(defmethod process-registry-event :job-accept-work-contract
  [{:keys [args] :as event}]
  (go-try
   (let [job-index (-> args :event_data first bn/number)
         work-index (-> args :event_data second bn/number)
         timestamp (-> args :timestamp bn/number)
         job-address (<!-<throw (contract.job-factory/job-store-by-index job-index))
         work-address (<!-<throw (contract.job/work-contract-by-index job-address work-index))
         contract-status (<!-<throw (contract.work-contract/contract-status work-address))
         date-updated (<!-<throw (contract.work-contract/date-updated work-address))]
     (model.job/update-work-contract!
      {:job/index job-index
       :work-contract/index work-index
       :work-contract/contract-status contract-status
       :work-contract/date-updated (bn/number date-updated)}))))


(defmethod process-registry-event :job-proceed-work-contract
  [{:keys [args] :as event}]
  (go-try
   (let [job-index (-> args :event_data first bn/number)
         work-index (-> args :event_data second bn/number)
         timestamp (-> args :timestamp bn/number)
         job-address (<!-<throw (contract.job-factory/job-store-by-index job-index))
         work-address (<!-<throw (contract.job/work-contract-by-index job-address work-index))
         contract-status (<!-<throw (contract.work-contract/contract-status work-address))
         date-updated (<!-<throw (contract.work-contract/date-updated work-address))]
     (model.job/update-work-contract!
      {:job/index job-index
       :work-contract/index work-index
       :work-contract/contract-status contract-status
       :work-contract/date-updated (bn/number date-updated)}))))


(defmethod process-registry-event :invoice-created
  [{:keys [args] :as event}]
  (go-try
   (let [job-index (-> args :event_data first bn/number)
         work-index (-> args :event_data second bn/number)
         invoice-index (-> args :event_data (nth 2) bn/number)
         job-address (<!-<throw (contract.job-factory/job-store-by-index job-index))
         work-address (<!-<throw (contract.job/work-contract-by-index job-address work-index))
         invoice-address (<!-<throw (contract.work-contract/invoice-by-index work-address invoice-index))
         date-created (-> (contract.invoice/date-created invoice-address) <!-<throw bn/number)
         date-updated (-> (contract.invoice/date-updated invoice-address) <!-<throw bn/number)
         amount-requested (-> (contract.invoice/amount-requested invoice-address) <!-<throw bn/number)
         invoice-data {:job/index job-index
                       :work-contract/index work-index
                       :invoice/index invoice-index
                       :invoice/date-created date-created
                       :invoice/date-updated date-updated
                       :invoice/amount-requested amount-requested}]
     (model.job/create-invoice! invoice-data))))


(defmethod process-registry-event :invoice-paid
  [{:keys [args] :as event}]
  (go-try
   (let [job-index (-> args :event_data first bn/number)
         work-index (-> args :event_data second bn/number)
         invoice-index (-> args :event_data (nth 2) bn/number)
         job-address (<!-<throw (contract.job-factory/job-store-by-index job-index))
         work-address (<!-<throw (contract.job/work-contract-by-index job-address work-index))
         invoice-address (<!-<throw (contract.work-contract/invoice-by-index work-address invoice-index))
         date-updated (-> (contract.invoice/date-updated invoice-address) <!-<throw bn/number)
         date-paid (-> (contract.invoice/date-paid invoice-address) <!-<throw bn/number)
         amount-paid (-> (contract.invoice/amount-paid invoice-address) <!-<throw bn/number)
         invoice-data {:job/index job-index
                       :work-contract/index work-index
                       :invoice/index invoice-index
                       :invoice/date-paid date-paid
                       :invoice/date-updated date-updated
                       :invoice/amount-paid amount-paid}]
     (model.job/update-invoice! invoice-data))))


(defmethod process-registry-event :dispute-created
  [{:keys [args] :as event}]
  (go-try
   (let [job-index (-> args :event_data first bn/number)
         work-index (-> args :event_data second bn/number)
         dispute-index (-> args :event_data (nth 2) bn/number)
         job-address (<!-<throw (contract.job-factory/job-store-by-index job-index))
         work-address (<!-<throw (contract.job/work-contract-by-index job-address work-index))
         dispute-address (<!-<throw (contract.work-contract/dispute-by-index work-address dispute-index))
         date-created (-> (contract.dispute/date-created dispute-address) <!-<throw bn/number)
         date-updated (-> (contract.dispute/date-updated dispute-address) <!-<throw bn/number)
         reason (<!-<throw (contract.dispute/reason dispute-address))
         dispute-data {:job/index job-index
                       :work-contract/index work-index
                       :dispute/index dispute-index
                       :dispute/reason reason
                       :dispute/date-created date-created
                       :dispute/date-updated date-updated}]
     (model.job/create-dispute! dispute-data))))


(defmethod process-registry-event :dispute-resolved
  [{:keys [args] :as event}]
  (go-try
   (let [job-index (-> args :event_data first bn/number)
         work-index (-> args :event_data second bn/number)
         dispute-index (-> args :event_data (nth 2) bn/number)
         job-address (<!-<throw (contract.job-factory/job-store-by-index job-index))
         work-address (<!-<throw (contract.job/work-contract-by-index job-address work-index))
         dispute-address (<!-<throw (contract.work-contract/dispute-by-index work-address dispute-index))
         date-updated (-> (contract.dispute/date-updated dispute-address) <!-<throw bn/number)
         date-resolved (-> (contract.dispute/date-resolved dispute-address) <!-<throw bn/number)
         employer-resolution-amount (-> (contract.dispute/employer-resolution-amount dispute-address) <!-<throw bn/number)
         candidate-resolution-amount (-> (contract.dispute/candidate-resolution-amount dispute-address) <!-<throw bn/number)
         arbiter-resolution-amount (-> (contract.dispute/arbiter-resolution-amount dispute-address) <!-<throw bn/number)
         dispute-data {:job/index job-index
                       :work-contract/index work-index
                       :dispute/index dispute-index
                       :dispute/date-resolved date-resolved
                       :dispute/date-updated date-updated
                       :dispute/employer-resolution-amount employer-resolution-amount
                       :dispute/candidate-resolution-amount candidate-resolution-amount
                       :dispute/arbiter-resolution-amount arbiter-resolution-amount}]
     (model.job/update-dispute! dispute-data))))


(defmethod process-registry-event :comment-created
  [{:keys [args] :as event}]
  (go-try
   (let [comment-address (:event_sender args)
         job-index (-> args :event_data first bn/number)
         work-index (-> args :event_data second bn/number)
         date-created (-> (contract.comment/date-created comment-address) <!-<throw bn/number)
         date-updated (-> (contract.comment/date-updated comment-address) <!-<throw bn/number)
         user-type (<!-<throw (contract.comment/user-type comment-address))
         user-id (-> (contract.comment/user-address comment-address) model.user/user-id)
         ipfs-data (<!-<throw (ipfs/get-edn (<!-<throw (contract.comment/last comment-address))))

         comment-data
         (assoc ipfs-data
                :job/index job-index
                :work-contract/index work-index
                :comment/revision 0
                :comment/date-created date-created
                :comment/date-updated date-updated
                :comment/user-type user-type
                :user/id user-id)]

     (condp = (<!-<throw (contract.comment/comment-type comment-address))
       ::enum.comment-type/work-contract
       (model.comment/create-work-contract-comment!
        (assoc comment-data
               :comment/index (-> args :event_data (nth 2) bn/number)))
       
       ::enum.comment-type/invoice
       (model.comment/create-invoice-comment!
        (assoc comment-data
               :invoice/index (-> args :event_data (nth 2) bn/number)
               :comment/index (-> args :event_data (nth 3) bn/number)))

       ::enum.comment-type/dispute
       (model.comment/create-dispute-comment!
        (assoc comment-data
               :dispute/index (-> args :event_data (nth 2) bn/number)
               :comment/index (-> args :event_data (nth 3) bn/number)))))))


(defmethod process-registry-event :comment-updated
  [{:keys [args] :as event}]
  (go-try
   (let [comment-address (:event_sender args)
         job-index (-> args :event_data first bn/number)
         work-index (-> args :event_data second bn/number)
         date-created (-> (contract.comment/date-created comment-address) <!-<throw bn/number)
         date-updated (-> (contract.comment/date-updated comment-address) <!-<throw bn/number)
         user-type (<!-<throw (contract.comment/user-type comment-address))
         user-id (-> (contract.comment/user-address comment-address) <!-<throw model.user/user-id)
         ipfs-data (<!-<throw (ipfs/get-edn (<!-<throw (contract.comment/last comment-address))))
         revision (-> (contract.comment/count comment-address) <!-<throw bn/number)

         comment-data
         (assoc ipfs-data
                :job/index job-index
                :work-contract/index work-index
                :comment/revision revision
                :comment/date-created date-created
                :comment/date-updated date-updated
                :comment/user-type user-type
                :user/id user-id)]

     (condp = (<!-<throw (contract.comment/comment-type comment-address))
       ::enum.comment-type/work-contract
       (model.comment/create-work-contract-comment!
        (assoc comment-data
               :comment/index (-> args :event_data (nth 2) bn/number)))
       
       ::enum.comment-type/invoice
       (model.comment/create-invoice-comment!
        (assoc comment-data
               :invoice/index (-> args :event_data (nth 2) bn/number)
               :comment/index (-> args :event_data (nth 3) bn/number)))

       ::enum.comment-type/dispute
       (model.comment/create-dispute-comment!
        (assoc comment-data
               :dispute/index (-> args :event_data (nth 2) bn/number)
               :comment/index (-> args :event_data (nth 3) bn/number)))))))


(defmethod process-registry-event :feedback-created
  [{:keys [args] :as event}]
  (go-try
   (let [feedback-address (:event_sender args)
         job-index (-> args :event_data first bn/number)
         work-index (-> args :event_data second bn/number)
         feedback-index (-> args :event_data (nth 2) bn/number)]
     (contract.feedback/with-ethlance-feedback feedback-address
       (let [{:keys [from-user-address
                     to-user-address
                     from-user-type
                     to-user-type
                     metahash
                     rating
                     date-created
                     date-updated]}
             (contract.feedback/feedback-by-index feedback-index)
             from-user-id (model.user/user-id from-user-address)
             to-user-id (model.user/user-id to-user-address)
             ipfs-data (<!-<throw (ipfs/get-edn metahash))]
         (model.feedback/create-feedback!
          (merge
           ipfs-data
           {:job/index job-index
            :work-contract/index work-index
            :feedback/index feedback-index
            :feedback/to-user-type to-user-type
            :feedback/to-user-id to-user-id
            :feedback/from-user-type from-user-type
            :feedback/from-user-id from-user-id
            :feedback/date-created date-created
            :feedback/date-updated date-updated
            :feedback/rating rating})))))))


(defmethod process-registry-event :feedback-updated
  [{:keys [args] :as event}]
  (go-try
   (let [feedback-address (:event_sender args)
         job-index (-> args :event_data first bn/number)
         work-index (-> args :event_data second bn/number)
         feedback-index (-> args :event_data (nth 2) bn/number)]
     (contract.feedback/with-ethlance-feedback feedback-address
       (let [{:keys [from-user-address
                     to-user-address
                     from-user-type
                     to-user-type
                     metahash
                     rating
                     date-created
                     date-updated]}
             (contract.feedback/feedback-by-index feedback-index)
             from-user-id (model.user/user-id from-user-address)
             to-user-id (model.user/user-id to-user-address)
             ipfs-data (<!-<throw (ipfs/get-edn metahash))]
         (model.feedback/update-feedback!
          (merge
           ipfs-data
           {:job/index job-index
            :work-contract/index work-index
            :feedback/index feedback-index
            :feedback/to-user-type to-user-type
            :feedback/to-user-id to-user-id
            :feedback/from-user-type from-user-type
            :feedback/from-user-id from-user-id
            :feedback/date-created date-created
            :feedback/date-updated date-updated
            :feedback/rating rating})))))))
