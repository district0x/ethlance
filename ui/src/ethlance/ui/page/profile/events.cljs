(ns ethlance.ui.page.profile.events
  (:require
    [district.ui.notification.events :as notification.events]
    [district.ui.router.effects :as router.effects]
    [district.ui.smart-contracts.queries :as contract-queries]
    [district.ui.web3-tx.events :as web3-events]
    [ethlance.shared.utils :refer [base58->hex]]
    [ethlance.ui.event.utils :as event.utils]
    [re-frame.core :as re]))


;; Page State
(def state-key :page.profile)


(def state-default
  {:pagination-limit 5
   :pagination-offset 0})


(defn initialize-page
  "Event FX Handler. Setup listener to dispatch an event when the page is active/visited."
  [{:keys [db]} _]
  {::router.effects/watch-active-page
   [{:id :page.profile/initialize-page
     :name :route.user/profile
     :dispatch []}]
   :db (assoc db state-key state-default)})


(defn clear-forms
  [db]
  (let [field-names [:invitation-text
                     :job-for-invitation]]
    (reduce (fn [acc field] (assoc-in acc [state-key field] nil)) db field-names)))


;;
;; Registered Events
;;
(def create-assoc-handler (partial event.utils/create-assoc-handler state-key))

(re/reg-event-fx :page.profile/initialize-page initialize-page)
(re/reg-event-fx :page.profile/set-job-for-invitation (create-assoc-handler :job-for-invitation))
(re/reg-event-fx :page.profile/set-invitation-text (create-assoc-handler :invitation-text))
(re/reg-event-fx :page.profile/set-pagination-offset (create-assoc-handler :pagination-offset))


(re/reg-event-fx
  :page.profile/invite-candidate
  (fn [_cofx [_ invitation-data]]
    (let [ipfs-invitation {:candidate (:candidate invitation-data)
                           :employer (:employer invitation-data)
                           :job-story-message/type :invitation
                           :job/id (get-in invitation-data [:job :job/id])
                           :message/creator (:employer invitation-data)
                           :text (:text invitation-data)}]
      {:data/upload {:data ipfs-invitation
                     :on-success [:invitation-to-ipfs-success ipfs-invitation]
                     :on-error [:invitation-to-ipfs-failure ipfs-invitation]}})))


(re/reg-event-fx
  :invitation-to-ipfs-success
  (fn [{:keys [db]} [_event ipfs-invitation ipfs-event]]
    (let [creator (:employer ipfs-invitation)
          ipfs-hash (base58->hex (:Hash ipfs-event))
          job-contract-address (:job/id ipfs-invitation)
          candidate (:candidate ipfs-invitation)
          tx-opts {:from creator}]
      {:dispatch [::web3-events/send-tx
                  {:instance (contract-queries/instance db :job job-contract-address)
                   :fn :add-candidate
                   :args [candidate ipfs-hash]
                   :tx-opts tx-opts
                   :tx-hash [::tx-hash]
                   :on-tx-hash-error [::tx-hash-error]
                   :on-tx-success [::invite-candidate-tx-success]
                   :on-tx-error [::invite-candidate-tx-failure]}]})))


(re/reg-event-fx
  :page.profile/invite-arbiter
  (fn [cofx [_ event-data]]
    (let [job-address (get-in event-data [:job :job/id])
          arbiter-address (:arbiter event-data)
          employer-address (:employer event-data)
          instance (contract-queries/instance (:db cofx) :job job-address)
          tx-opts {:from employer-address}
          contract-args [employer-address [arbiter-address]]]
      {:dispatch [::web3-events/send-tx
                  {:instance instance
                   :fn :invite-arbiters
                   :args contract-args
                   :tx-opts tx-opts
                   :tx-hash [::arbitration-tx-hash]
                   :on-tx-hash-error [::invite-arbiters-tx-hash-error]
                   :on-tx-success [::invite-arbiter-tx-success]
                   :on-tx-error [::invite-arbiters-tx-error]}]})))


(re/reg-event-db
  ::tx-hash-error
  (fn [_db event]
    (println ">>>ui.page.profile ::tx-hash-error" event)))


(re/reg-event-db
  ::invitation-to-ipfs-failure
  (fn [_db event]
    (println ">>> :invitation-to-ipfs-failure" event)))


(re/reg-event-fx
  ::invite-candidate-tx-success
  (fn [{:keys [db]} _event]
    {:db (clear-forms db)
     :dispatch [::notification.events/show "Transaction to invite candidate processed successfully"]}))


(re/reg-event-db
  ::invite-candidate-tx-failure
  (fn [_db event]
    (println ">>> ::invite-candidate-tx-failure" event)))


(re/reg-event-fx
  ::invite-arbiter-tx-success
  (fn [{:keys [db]} _event]
    {:db (clear-forms db)
     :dispatch [::notification.events/show "Transaction to invite arbiter processed successfully"]}))
