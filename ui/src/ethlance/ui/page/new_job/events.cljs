(ns ethlance.ui.page.new-job.events
  (:require
    [alphabase.hex :as hex]
    [district.ui.router.effects :as router.effects]
    [district.ui.router.events :as router-events]
    [ethlance.ui.event.utils :as event.utils]
    [ethlance.shared.utils :refer [eth->wei base58->hex]]
    [re-frame.core :as re]
            ["web3" :as w3]
    [district.ui.smart-contracts.queries :as contract-queries]
    [district.ui.web3-accounts.queries :as accounts-queries]
    [district.ui.web3-tx.events :as web3-events]
    [district.ui.notification.events :as notification.events]
    [ethlance.shared.contract-constants :as contract-constants]))

(def state-key :page.new-job)
(def interceptors [re/trim-v])

(def state-default
  {:job/title "Rauamaak on meie saak"
   :job/description "Tee t88d ja n2e vaeva"
   :job/category "Admin Support"
   :job/bid-option :hourly-rate
   :job/required-experience-level :intermediate
   :job/estimated-project-length :day
   :job/required-availability :full-time
   :job/required-skills #{"Somali" "Solidity"}
   :job/token-type :eth
   :job/token-amount 0.69
   :job/token-address "0x1111111111111111111111111111111111111111"
   :job/token-id 0
   :job/with-arbiter? false})

(defn initialize-page
  "Event FX Handler. Setup listener to dispatch an event when the page is active/visited."
  []
  {::router.effects/watch-active-page
   [{:id :page.new-job/initialize-page
     :name :route.job/new
     :dispatch [:page.new-job/auto-fill-form]}]})

(def create-assoc-handler (partial event.utils/create-assoc-handler state-key))

(re/reg-event-db
  :page.new-job/auto-fill-form
  (fn [db]
    (assoc-in db [state-key] state-default)))

(re/reg-event-fx :page.new-job/initialize-page initialize-page)
(re/reg-event-fx :page.new-job/set-bid-option (create-assoc-handler :job/bid-option))
(re/reg-event-fx :page.new-job/set-category (create-assoc-handler :job/category))
(re/reg-event-fx :page.new-job/set-description (create-assoc-handler :job/description))
(re/reg-event-fx :page.new-job/set-estimated-project-length (create-assoc-handler :job/estimated-project-length))
(re/reg-event-fx :page.new-job/set-title (create-assoc-handler :job/title))
(re/reg-event-fx :page.new-job/set-required-availability (create-assoc-handler :job/required-availability))
(re/reg-event-fx :page.new-job/set-required-experience-level (create-assoc-handler :job/required-experience-level))
(re/reg-event-fx :page.new-job/set-required-skills (create-assoc-handler :job/required-skills))
(re/reg-event-fx :page.new-job/set-with-arbiter? (create-assoc-handler :job/with-arbiter?))
(re/reg-event-fx :page.new-job/invite-arbiter (create-assoc-handler :job/invited-arbiter))

(re/reg-event-fx :page.new-job/set-token-type (create-assoc-handler :job/token-type))
(re/reg-event-fx :page.new-job/set-token-amount (create-assoc-handler :job/token-amount))
(re/reg-event-fx :page.new-job/set-token-address (create-assoc-handler :job/token-address))
(re/reg-event-fx :page.new-job/set-token-id (create-assoc-handler :job/token-id))

(def db->ipfs-mapping
  {:job/bid-option :job/bid-option
   :job/category :job/category
   :job/description :job/description
   :job/estimated-project-length :job/estimated-project-length
   :job/required-experience-level :job/required-experience-level
   :job/required-availability :job/required-availability
   :job/required-skills :job/required-skills
   :job/title :job/title
   :job/invited-arbiter :job/invited-arbiter})

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
      {:ipfs/call {:func "add"
                   :args [(js/Blob. [ipfs-job])]
                   :on-success [:job-to-ipfs-success]
                   :on-error [:job-to-ipfs-failure]}})))

(re/reg-event-fx
  :job-to-ipfs-success
  (fn [cofx event]
    (println ">>> job-to-ipfs-success" event)
    (let [creator (accounts-queries/active-account (:db cofx))
          job-fields (get-in cofx [:db state-key])
          token-type (:job/token-type job-fields)
          token-amount (if (= token-type :eth)
                         (eth->wei (:job/token-amount job-fields))
                         (:job/token-amount job-fields))
          address-placeholder "0x0000000000000000000000000000000000000000"
          token-address (if (not (= token-type :eth))
                          (:job/token-address job-fields)
                          address-placeholder)
          offered-value {:value token-amount
                         :token
                         {:tokenId (:job/token-id job-fields)
                          :tokenContract
                          {:tokenType (contract-constants/token-type->enum-val token-type)
                           :tokenAddress token-address}}}
          tx-opts {:from creator :gas 10000000}
          tx-opts-with-value (if (= token-type :eth)
                               (merge tx-opts {:value token-amount})
                               tx-opts)
          invited-arbiters (if (:job/with-arbiter? job-fields)
                             [(:job/invited-arbiter job-fields)]
                             [])
          ipfs-response (get-in event [:event 1])
          ipfs-hash (base58->hex (get-in event [1 :Hash]))]
      {:dispatch [::web3-events/send-tx
                  {:instance (contract-queries/instance (:db cofx) :ethlance)
                   :fn :createJob
                   :args [creator [(clj->js offered-value)] invited-arbiters ipfs-hash]
                   :tx-opts tx-opts-with-value
                   :tx-hash [::tx-hash]
                   :on-tx-hash-error [::tx-hash-error]
                   :on-tx-success [::create-job-tx-success]
                   :on-tx-error [::create-job-tx-error]}]})))

; TODO: fix event/callback names in README (they don't have on-<...> prefix)
;         https://github.com/district0x/re-frame-web3-fx#usage
(re/reg-event-fx
  ::tx-hash
  (fn [db event] (println ">>> ethlance.ui.page.new-job.events :tx-hash" event)))

(re/reg-event-fx
  ::web3-tx-localstorage
  (fn [db event] (println ">>> ethlance.ui.page.new-job.events :web3-tx-localstorage" event)))

(re/reg-event-fx
  ::create-job-tx-success
  (fn [{:keys [db]} [event-name tx-data]]
    (re/dispatch [::router-events/navigate
                  :route.job/detail
                  {:id (get-in tx-data [:events :Job-created :return-values :job])}])
    {:dispatch [::notification.events/show "Transaction to create job processed successfully"]}))

(re/reg-event-db
  ::create-job-tx-error
  (fn [db event]
    {:dispatch [::notification.events/show "Error with creating new job transaction"]}))

(re/reg-event-fx
  ::job-to-ipfs-failure
  (fn [_ _]
  {:dispatch [::notification.events/show "Error with loading new job data to IPFS"]}))
