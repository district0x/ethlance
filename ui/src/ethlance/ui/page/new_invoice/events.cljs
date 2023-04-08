(ns ethlance.ui.page.new-invoice.events
  (:require [district.parsers :refer [parse-float parse-int]]
            [district.ui.router.effects :as router.effects]
            [ethlance.shared.utils :refer [eth->wei base58->hex]]
            [ethlance.ui.event.utils :as event.utils]
            [ethlance.ui.util.tokens :as util.tokens]
            [district.ui.web3-tx.events :as web3-events]
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
  {:job-listing ["Smart Contract" "USD" "ETH"]
   :invoiced-job nil
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
(re/reg-event-fx :page.new-invoice/set-job-listing (create-assoc-handler :job-listing))
(re/reg-event-fx :page.new-invoice/set-hours-worked (create-assoc-handler :hours-worked parse-int))
(re/reg-event-fx :page.new-invoice/set-hourly-rate (create-assoc-handler :hourly-rate parse-float))
(re/reg-event-fx :page.new-invoice/set-invoice-amount (create-assoc-handler :invoice-amount parse-float))
(re/reg-event-fx :page.new-invoice/set-message (create-assoc-handler :message))

(re/reg-event-fx
  :page.new-invoice/set-invoiced-job
  (fn [cofx [_ job]]
    {:db (assoc-in (:db cofx) [state-key :invoiced-job] job)
     :dispatch [:district.ui.conversion-rates.events/load-conversion-rates
                {:from-currencies [(get-in job [:job :token-details :token-detail/symbol])]
                 :to-currencies [:USD]}]}))

(re/reg-event-fx
  :page.new-invoice/send
  (fn [{:keys [db]}]
    (let [db-invoice (get-in db [state-key])
          ipfs-invoice {:invoice/hours-worked (:hours-worked db-invoice)
                        :message/text (:message db-invoice)
                        :job/id (-> db-invoice :invoiced-job :job/id)
                        :job-story/id (-> db-invoice :invoiced-job :job-story/id parse-int)}]
      {:ipfs/call {:func "add"
                   :args [(js/Blob. [ipfs-invoice])]
                   :on-success [:invoice-to-ipfs-success]
                   :on-error [:invoice-to-ipfs-failure]}})))

(re/reg-event-fx
  :invoice-to-ipfs-success
  (fn [cofx event]
    (let [invoice-fields (get-in cofx [:db state-key])
          job-fields (-> invoice-fields :invoiced-job :job)
          contract-address (:job/id job-fields)
          creator (accounts-queries/active-account (:db cofx))
          token-type (keyword (:job/token-type job-fields))
          invoice-amount (:invoice-amount invoice-fields)
          token-amount (if (= token-type :eth)
                         (eth->wei invoice-amount)
                         invoice-amount)
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
          ipfs-response (get-in event [:event 1])
          ipfs-hash (base58->hex (get-in event [1 :Hash]))]
      {:dispatch [::web3-events/send-tx
                  {:instance (contract-queries/instance (:db cofx) :job contract-address)
                   :fn :createInvoice
                   :args [[(clj->js offered-value)] ipfs-hash]
                   :tx-opts tx-opts
                   :tx-hash [::tx-hash]
                   :on-tx-hash-n [[::tx-hash]]
                   :on-tx-hash-error [::tx-hash-error]
                   :on-tx-hash-error-n [[::tx-hash-error]]
                   :on-tx-success [::send-invoice-tx-success]
                   :on-tx-success-n [[::send-invoice-tx-success]]
                   :on-tx-error [::send-invoice-tx-error]
                   :on-tx-error-n [[::send-invoice-tx-error]]}]})))

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

(re/reg-event-db
  ::send-invoice-tx-success
  (fn [db [event-name tx-data]]
    (let [web3 (web3-queries/web3 db)
          contract-instance (smart-contracts.queries/instance db :ethlance)
          raw-event (get-in tx-data [:events :0 :raw])
          invoice-created (util.tokens/parse-event web3 contract-instance raw-event :Invoice-created)]
      ; TODO: might have to subscribe to contract event because on test/mainnet transactions take time to be mined
      (re/dispatch [::router-events/navigate
                    :route.invoice/index
                    {:job-id (:job invoice-created) :invoice-id (:invoice-id invoice-created)}]))))

(re/reg-event-db
  ::send-invoice-tx-error
  (fn [db event]
    (println ">>> got :create-job-tx-error event:" event)))
