(ns ethlance.ui.page.invoices.events
  (:require [district.ui.router.effects :as router.effects]
            [ethlance.ui.event.utils :as event.utils]
            [ethlance.ui.util.tokens :as util.tokens]
            [district.ui.smart-contracts.queries :as contract-queries]
            [ethlance.shared.utils :refer [eth->wei base58->hex]]
            [district.ui.web3.queries :as web3-queries]
            [district.ui.web3-tx.events :as web3-events]
            [re-frame.core :as re]))

;; Page State
(def state-key :page.invoices)
(def state-default
  {})

(defn initialize-page
  "Event FX Handler. Setup listener to dispatch an event when the page is active/visited."
  [{:keys []} _]
  {::router.effects/watch-active-page
   [{:id :page.invoices/initialize-page
     :name :route.invoice/index
     :dispatch []}]})

;;
;; Registered Events
;;
(def create-assoc-handler (partial event.utils/create-assoc-handler state-key))


;; TODO: switch based on dev environment
(re/reg-event-fx :page.invoices/initialize-page initialize-page)

(re/reg-event-fx
  :page.invoices/pay
  (fn [{:keys [db]} [_ invoice]]
    (println ">>> page.invoices/pay" invoice)
    (let []
      {:ipfs/call {:func "add"
                   :args [(js/Blob. [invoice])]
                   :on-success [::invoice-to-ipfs-success invoice]
                   :on-error [::invoice-to-ipfs-failure invoice]}})))

(re/reg-event-fx
  ::invoice-to-ipfs-success
  (fn [cofx [_event invoice ipfs-event]]
    (println ">>> invoice-to-ipfs-success" {:invoice invoice :ipfs-event ipfs-event})
    (let [invoice-fields (get-in cofx [:db state-key])
          contract-address (:job/id invoice)
          tx-opts {:from (:payer invoice) :gas 10000000}
          invoice-id (:invoice-id invoice)
          ipfs-hash (-> ipfs-event :Hash base58->hex)]
      {:dispatch [::web3-events/send-tx
                  {:instance (contract-queries/instance (:db cofx) :job contract-address)
                   :fn :pay-invoice
                   :args [invoice-id ipfs-hash]
                   :tx-opts tx-opts
                   :tx-hash [::tx-hash]
                   :on-tx-hash-error [::tx-hash-error]
                   :on-tx-success [::send-invoice-tx-success invoice]
                   :on-tx-error [::send-invoice-tx-error invoice]}]})))

(re/reg-event-db
  ::invoice-to-ipfs-failure
  (fn [db event]
    (println ">>> ethlance.ui.page.invoices.events EVENT :invoice-to-ipfs-failure" event)
    db))

(re/reg-event-fx
  ::tx-hash
  (fn [db event] (println ">>> ethlance.ui.page.invoices.events :tx-hash" event)))

(re/reg-event-fx
  ::web3-tx-localstorage
  (fn [db event] (println ">>> ethlance.ui.page.invoices.events :web3-tx-localstorage" event)))

(re/reg-event-db
  ::send-invoice-tx-success
  (fn [db _]
    (println ">>> ::send-invoice-tx-success refetching invoice")
    {:fx [[:dispatch [:page.invoices/refetch-invoice]]]}))

(re/reg-event-db
  ::send-invoice-tx-error
  (fn [db event]
    (println ">>> got :create-job-tx-error event:" event)))
