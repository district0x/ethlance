(ns ethlance.ui.page.invoices.events
  (:require
    [district.ui.notification.events :as notification.events]
    [district.ui.router.effects :as router.effects]
    [district.ui.smart-contracts.queries :as contract-queries]
    [district.ui.web3-tx.events :as web3-events]
    [ethlance.shared.utils :refer [base58->hex]]
    [re-frame.core :as re]))


;; Page State
(def state-key :page.invoices)


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

;; TODO: switch based on dev environment
(re/reg-event-fx :page.invoices/initialize-page initialize-page)


(re/reg-event-fx
  :page.invoices/pay
  (fn [_ [_ invoice]]
    {:data/upload {:data invoice
                   :on-success [::invoice-to-ipfs-success invoice]
                   :on-error [::invoice-to-ipfs-failure invoice]}}))


(re/reg-event-fx
  ::invoice-to-ipfs-success
  (fn [cofx [_event invoice ipfs-event]]
    (let [contract-address (:job/id invoice)
          tx-opts {:from (:payer invoice)}
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


(re/reg-event-fx
  ::invoice-to-ipfs-failure
  (fn [_ _]
    {:dispatch [::notification.events/show "Error uploading invoice data to IPFS"]}))


(re/reg-event-fx
  ::tx-hash
  (fn [_ event] (println ">>> ethlance.ui.page.invoices.events :tx-hash" event)))


(re/reg-event-fx
  ::send-invoice-tx-success
  (fn [_ _]
    {:fx [[:dispatch [:page.invoices/refetch-invoice]]
          [:dispatch [::notification.events/show "Invoice transaction processed sucessfully"]]]}))


(re/reg-event-fx
  ::send-invoice-tx-error
  (fn [_ _]
    {:dispatch [::notification.events/show "Error with send invoice transaction"]}))
