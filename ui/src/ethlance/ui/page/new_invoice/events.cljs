(ns ethlance.ui.page.new-invoice.events
  (:require [district.parsers :refer [parse-float parse-int]]
            [district.ui.router.effects :as router.effects]
            [ethlance.shared.utils :refer [eth->wei base58->hex]]
            [ethlance.ui.event.utils :as event.utils]
            [ethlance.ui.util.tokens :as util.tokens]
            [district.ui.web3-tx.events :as web3-events]
            [district.ui.notification.events :as notification.events]
            [district.ui.router.events :as router-events]
            [district.ui.smart-contracts.queries :as contract-queries]
            [ethlance.shared.contract-constants :as contract-constants]
            [district.ui.web3-accounts.queries :as accounts-queries]
            [re-frame.core :as re]

            ; TODO: extract for Event decoding

            [district.ui.smart-contracts.queries :as smart-contracts.queries]
            [district.ui.web3.queries :as web3-queries]
            [cljs-web3-next.eth :as web3-eth]
            [cljs-web3-next.helpers :as web3-helpers]
            ))

(def state-key :page.new-invoice)

(def state-default
  {:invoiced-job nil
   :hours-worked nil
   :hourly-rate nil
   :invoice-amount nil
   :message nil})

(def create-assoc-handler (partial event.utils/create-assoc-handler state-key))

(defn initialize-page
  "Event FX Handler. Setup listener to dispatch an event when the page is active/visited."
  []
  {::router.effects/watch-active-page
   [{:id :page.new-invoice/initialize-page
     :name :route.invoice/new
     :dispatch []}]})

(re/reg-event-fx :page.new-invoice/initialize-page initialize-page)
(re/reg-event-fx :page.new-invoice/set-hours-worked (create-assoc-handler :hours-worked parse-int))
(re/reg-event-fx :page.new-invoice/set-hourly-rate (create-assoc-handler :hourly-rate parse-float))
(re/reg-event-fx :page.new-invoice/set-invoice-amount (create-assoc-handler :invoice-amount))
(re/reg-event-fx :page.new-invoice/set-message (create-assoc-handler :message))

(re/reg-event-fx
  :page.new-invoice/set-invoiced-job
  (fn [cofx [_ job]]
    (let [token-type (get-in job [:job :token-details :token-detail/type])
          updated-cofx {:db (assoc-in (:db cofx) [state-key :invoiced-job] job)}
          load-eth-rate [:dispatch [:district.ui.conversion-rates.events/load-conversion-rates
                                    {:from-currencies [token-type] :to-currencies [:USD]}]]]
      (if (= token-type :eth)
        (assoc-in updated-cofx [:fx] [load-eth-rate])
        updated-cofx))))

(re/reg-event-fx
  :page.new-invoice/send
  (fn [{:keys [db]}]
    (let [db-invoice (get-in db [state-key])
          ipfs-invoice {:invoice/amount-requested (get-in db-invoice [:invoice-amount :token-amount])
                        :invoice/hours-worked (:hours-worked db-invoice)
                        :invoice/hourly-rate (:hourly-rate db-invoice)
                        :message/text (:message db-invoice)
                        :job/id (-> db-invoice :invoiced-job :job/id)
                        :job-story/id (-> db-invoice :invoiced-job :job-story/id parse-int)}]
      {:ipfs/call {:func "add"
                   :args [(js/Blob. [ipfs-invoice])]
                   :on-success [:invoice-to-ipfs-success ipfs-invoice]
                   :on-error [:invoice-to-ipfs-failure ipfs-invoice]}})))

(re/reg-event-fx
  :invoice-to-ipfs-success
  (fn [cofx [_event ipfs-job ipfs-event]]
    (let [invoice-fields (get-in cofx [:db state-key])
          job-fields (-> invoice-fields :invoiced-job :job)
          contract-address (:job/id job-fields)
          creator (accounts-queries/active-account (:db cofx))
          token-type (keyword (:job/token-type job-fields))
          invoice-amount (get-in invoice-fields [:invoice-amount :token-amount])
          address-placeholder "0x0000000000000000000000000000000000000000"
          token-address (if (not (= token-type :eth))
                          (:job/token-address job-fields)
                          address-placeholder)
          offered-value {:value (str invoice-amount)
                         :token
                         {:tokenId (:job/token-id job-fields)
                          :tokenContract
                          {:tokenType (contract-constants/token-type->enum-val token-type)
                           :tokenAddress token-address}}}
          tx-opts {:from creator :gas 10000000}
          ipfs-hash (-> ipfs-event :Hash base58->hex)]
      {:dispatch [::web3-events/send-tx
                  {:instance (contract-queries/instance (:db cofx) :job contract-address)
                   :fn :create-invoice
                   :args [[(clj->js offered-value)] ipfs-hash]
                   :tx-opts tx-opts
                   :tx-hash [::tx-hash]
                   :on-tx-hash-error [::tx-hash-error]
                   :on-tx-success [::send-invoice-tx-success ipfs-job]
                   :on-tx-error [::send-invoice-tx-error ipfs-job]}]})))

(re/reg-event-db
  ::invoice-to-ipfs-failure
  (fn [db event]
    (println ">>> ethlance.ui.page.new-invoice.events EVENT :invoice-to-ipfs-failure" event)
    db))

(re/reg-event-fx
  ::tx-hash
  (fn [db event] (println ">>> ethlance.ui.page.new-invoice.events :tx-hash" event)))

(re/reg-event-fx
  ::web3-tx-localstorage
  (fn [db event] (println ">>> ethlance.ui.page.new-invoice.events :web3-tx-localstorage" event)))

(def invoice-data (atom nil))

(re/reg-event-fx
  ::send-invoice-tx-success
  (fn [{:keys [db]} [event-name ipfs-job tx-data]]
    (let [web3 (web3-queries/web3 db)
          contract-instance (smart-contracts.queries/instance db :ethlance)
          raw-event (get-in tx-data [:events :0 :raw])
          invoice-created (util.tokens/parse-event web3 contract-instance raw-event :Invoice-created)
          job-story-id (:job-story/id ipfs-job)]
      (re/dispatch [::router-events/navigate :route.job/contract {:job-story-id job-story-id}])
      {:dispatch [::notification.events/show "Transaction to create invoice processed successfully"]
       :db (assoc-in db [state-key] state-default)})))

(re/reg-event-db
  ::send-invoice-tx-error
  (fn [db event]
    (println ">>> got :create-job-tx-error event:" event)))
