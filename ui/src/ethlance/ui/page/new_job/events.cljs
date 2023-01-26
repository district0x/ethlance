(ns ethlance.ui.page.new-job.events
  (:require
    [alphabase.base58 :as base58]
    [alphabase.hex :as hex]
    [district.ui.router.effects :as router.effects]
    [ethlance.ui.graphql :as graphql]
    [ethlance.ui.event.utils :as event.utils]
    [re-frame.core :as re]
    ; [ethlance.ui.util.ipfs :as ipfs] ; Dowsn't exist
    [district.ui.smart-contracts.queries :as contract-queries]
    [district.ui.web3-accounts.queries :as accounts-queries]
    [district.ui.web3-tx.events :as web3-events]
    [ethlance.shared.contract-constants :as contract-constants]
    ))

(def state-key :page.new-job)
(def interceptors [re/trim-v])

(def state-default
  {:type :job
   :name nil
   :category nil
   :bid-option :hourly-rate
   :required-experience-level :intermediate
   :estimated-project-length :day
   :required-availability :full-time
   :required-skills #{}
   :description nil
   :form-of-payment :ethereum
   :token-address nil
   :with-arbiter? true})

(defn initialize-page
  "Event FX Handler. Setup listener to dispatch an event when the page is active/visited."
  []
  {::router.effects/watch-active-page
   [{:id :page.new-job/initialize-page
     :name :route.job/new
     :dispatch []}]})

(def create-assoc-handler (partial event.utils/create-assoc-handler state-key))

(re/reg-event-fx :page.new-job/initialize-page initialize-page)
(re/reg-event-fx :page.new-job/set-type (create-assoc-handler :type))
(re/reg-event-fx :page.new-job/set-name (create-assoc-handler :name))
(re/reg-event-fx :page.new-job/set-description (create-assoc-handler :description))
(re/reg-event-fx :page.new-job/set-category (create-assoc-handler :category))
(re/reg-event-fx :page.new-job/set-required-experience-level (create-assoc-handler :required-experience-level))
(re/reg-event-fx :page.new-job/set-bid-option (create-assoc-handler :bid-option))
(re/reg-event-fx :page.new-job/set-required-availability (create-assoc-handler :required-availability))
(re/reg-event-fx :page.new-job/set-estimated-project-length (create-assoc-handler :estimated-project-length))
(re/reg-event-fx :page.new-job/set-required-skills (create-assoc-handler :required-skills))
(re/reg-event-fx :page.new-job/set-form-of-payment (create-assoc-handler :form-of-payment))
(re/reg-event-fx :page.new-job/set-token-address (create-assoc-handler :token-address))
(re/reg-event-fx :page.new-job/set-with-arbiter? (create-assoc-handler :with-arbiter?))


(def db->ipfs-mapping
  {:job/title :name
   :job/description :description
   :job/category :category
   :job/expertise-level :required-experience-level
   :job/bid-option :bid-option
   :job/required-availability :required-availability
   :job/estimated-length :estimated-project-length
   :job/required-skills :skills})

(defn- db-job->ipfs-job
  "Useful for renaming map keys by reducing over a map of keyword -> keyword
  where: key is the name of the resulting map key and value is the key of the
  `job-data` map.

  Use `partial` to 'remember' the data object and get function signature
  suitable for `reduce`"
  [job-data acc ipfs-key db-key]
  (assoc acc ipfs-key (job-data db-key)))

(re/reg-event-fx
  :page.new-job/create
  [interceptors]
  (fn [{:keys [db]}]
    (let [db-job (get-in db [state-key])
          ipfs-job (reduce-kv (partial db-job->ipfs-job db-job) {} db->ipfs-mapping)]
      {:ipfs/call {:func "add" :args [(js/Blob. [ipfs-job])] :on-success [:job-to-ipfs-success] :on-error [:job-to-ipfs-failure]}})))

(defn base58->hex [base58-str]
  (->> base58-str
    base58/decode
    hex/encode
    (str "0x" ,,,)))

(re/reg-event-fx
  :job-to-ipfs-success
  (fn [cofx event]
    ; IPFS `event` param structure: [:job-to-ipfs-success {"Name":"blob","Hash":"QmT8k5NsMDDoeiXQz9ox5FfdCwHaf7ZYi9CZUyysXAu8TG","Size":"263"}]
    (let [creator (accounts-queries/active-account (:db cofx))
          funding-amount-wei (* 0.01 100000000000000)
          not-used-for-erc20 0
          offered-token-type (contract-constants/token-type :eth)
          placeholder-address "0x1111111111111111111111111111111111111111"
          offered-value {:token
                         {:tokenContract {:tokenType offered-token-type :tokenAddress placeholder-address}
                          :tokenId not-used-for-erc20} :value funding-amount-wei}

          job-type 1
          invited-arbiters []
          ipfs-response (->> (get-in event [1])
                         (.parse js/JSON ,,,)
                         (js->clj ,,,))
          ipfs-hash (base58->hex (get ipfs-response "Hash"))]
      {:dispatch [::web3-events/send-tx
                  {:instance (contract-queries/instance (:db cofx) :ethlance)
                   :fn :createJob
                   :args [creator [offered-value] job-type invited-arbiters ipfs-hash]
                   :tx-opts {:from creator :gas 4500000 :value (:value offered-value)}
                   :tx-hash [:tx-hash]
                   :on-tx-hash-n [[:tx-hash]]
                   :on-tx-hash-error [:tx-hash-error]
                   :on-tx-hash-error-n [[:tx-hash-error]]
                   :on-tx-success [:tx-success]
                   :on-tx-success-n [[:tx-success]]
                   :on-tx-error [:tx-error]
                   :on-tx-error-n [[:tx-error]]}]})))

; TODO: fix event/callback names in README (they don't have on-<...> prefix)
;         https://github.com/district0x/re-frame-web3-fx#usage
(re/reg-event-fx :tx-hash (fn [db event]         (println ">>>>>>!!!!! tx-hash" event)))
(re/reg-event-fx :web3-tx-localstorage (fn [db event]         (println ">>>>>>!!!!! web3-tx-localstorage" event)))

(re/reg-event-db
  :job-to-ipfs-failure
  (fn [db event]
    (println ">>> EVENT ze-new-job-failure" event)
    db))
