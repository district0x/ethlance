(ns ethlance.ui.page.dev.contract-ops
  (:require
    [district.ui.component.page :refer [page]]
    [ethlance.ui.component.main-layout :refer [c-main-layout]]
    [district.ui.router.subs :as router.subs]
    [re-frame.core :as re]
    [ethlance.ui.component.select-input :refer [c-select-input]]

    [district.ui.smart-contracts.queries :as contract-queries]
    [district.ui.web3-tx.events :as web3-events]
    [clojure.edn :as edn]))

; ------------ EVENTS ----------------
(def state-key :page.dev.contract-ops)

(re/reg-event-db
  :page.dev.contract-ops/set-token-amount
  (fn [db [_ amount]]
    (assoc-in db [state-key :token-amount] amount)))

(re/reg-event-db
  :page.dev.contract-ops/set-token-info
  (fn [db [_ id]]
    (assoc-in db [state-key :token-info] id)))

(def ze-tx-receipt (atom nil))
(re/reg-event-db
  ::mint-token-tx-success
  (fn [db [_ token-type tx-receipt]]
    (reset! ze-tx-receipt tx-receipt)
    (set! (.. js/window -zeTxReceipt) tx-receipt)
    (assoc-in db [state-key :minted-token-id]
              (case token-type
                :erc721 (get-in tx-receipt [:events :Transfer :return-values :tokenId])
                :erc1155 (get-in tx-receipt [:events :Transfer-single :return-values :id])
                nil))))

(re/reg-event-db
  ::mint-token-tx-error
  (fn [db [_ tx-receipt]]
    (println ">>> ::mint-token-tx-error" tx-receipt)))

(re/reg-event-fx
  :page.dev.contract-ops/send-mint-token-tx
  (fn [{:keys [db]} _]
    (let [recipient (district.ui.web3-accounts.queries/active-account db)
          amount (get-in db [state-key :token-amount])
          token-info (get-in db [state-key :token-info])
          _ (println ">>> SEND" token-info)
          token-type (:type token-info)
          contract-fn (case token-type
                        :erc20 :mint
                        :erc721 :award-item
                        :erc1155 :award-item)
          contract-args (case token-type
                          :erc20 [recipient amount]
                          :erc721 [recipient]
                          :erc1155 [recipient amount])
          contract-key (:contract token-info)]
      {:fx [[:dispatch
             [::web3-events/send-tx
              {:instance (contract-queries/instance db contract-key)
               :fn contract-fn
               :args contract-args
               :tx-opts {:from recipient}
               :on-tx-receipt [::mint-token-tx-success token-type]
               :on-tx-success [::mint-token-tx-success token-type]
               :on-tx-error [::mint-token-tx-error]}]]]})))

(re/reg-event-db
  :page.dev.contract-ops/update-app-db-query
  (fn [db [_ query]]
    (assoc-in db [state-key :app-db-query] query)))

(re/reg-event-db
  :page.dev.contract-ops/make-app-db-query
  (fn [db [_ query]]
    (assoc-in db [state-key :app-db-query-vector] (clojure.edn/read-string query))))

; ------------ SUBSCRIPTIONS ----------------
(re/reg-sub
  :page.dev.contract-ops/token-amount
  (fn [db _]
    (get-in db [state-key :token-amount])))

(re/reg-sub
  :page.dev.contract-ops/token-info
  (fn [db _]
    (get-in db [state-key :token-info])))

(re/reg-sub
  :page.dev.contract-ops/minted-token-id
  (fn [db _]
    (get-in db [state-key :minted-token-id])))

(re/reg-sub
  :page.dev.contract-ops/app-db-query-input
  (fn [db _]
    (get-in db [state-key :app-db-query])))

(re/reg-sub
  :page.dev.contract-ops/app-db-query-results
  (fn [db _]
    (get-in db (get-in db [state-key :app-db-query-vector]))))

(re/reg-sub
  :page.dev.contract-ops/app-db-result-keys
  :<- [:page.dev.contract-ops/app-db-query-results]
  (fn [query-result _]
    (if (map? query-result)
      (keys query-result)
      "Not a map")))

; ----------------- VIEWS -------------------

(defn c-app-db []
  (let [query-input (re/subscribe [:page.dev.contract-ops/app-db-query-input])
        query-results (re/subscribe [:page.dev.contract-ops/app-db-query-results])
        result-keys (re/subscribe [:page.dev.contract-ops/app-db-result-keys])
        ]
    [:div {:style {:margin "1em" :border "solid 1px"}}
     [:label "Enter query for app-db (ENTER to query)"]
     [:input {:value @query-input
              :on-change (fn [event]
                           (set! (.. js/window -zeOnChangeEvent) event)
                           (re/dispatch [:page.dev.contract-ops/update-app-db-query (-> event .-target .-value)]))
              :on-key-up (fn [event]
                           (if (= "Enter" (.-key event))
                                       (re/dispatch [:page.dev.contract-ops/make-app-db-query @query-input])))}]
     [:h3 {:style {:font-size "2em"}} "Keys"]
     [:pre {:style {:font-family "Monospace" :font-weight "bold"}}
      (with-out-str (cljs.pprint/pprint @result-keys))]
     [:h3 {:style {:font-size "2em"}} "Content"]
     [:pre {:style {:font-family "Monospace"}}
      (with-out-str (cljs.pprint/pprint @query-results))]]))

(defn c-mint-tokens []
  (let [token-amount (re/subscribe [:page.dev.contract-ops/token-amount])
        selected-token (re/subscribe [:page.dev.contract-ops/token-info])
        minted-token-id (re/subscribe [:page.dev.contract-ops/minted-token-id])
        tokens {:erc20 :token
                :erc721 :test-nft
                :erc1155 :test-multi-token}
        available-tokens (map (fn [[t-type t-key]]
                                (merge {:type t-type
                                        :contract t-key}
                                       (select-keys (ethlance.ui.config/contracts-var t-key) [:name :address])))
                              tokens)]
    [:div {:style {:border-style "dotted"}}
     (into [:fieldset] (map
                         (fn [token-info]
                           [:div
                            [:input {:type :radio
                                     :id (:name token-info)
                                     :value token-info
                                     :name "token-selection"
                                     :on-change #(re/dispatch [:page.dev.contract-ops/set-token-info token-info])}]
                            [:label {:for (:name token-info)} (str (name (:type token-info)) " " (:name token-info) " (" (:address token-info)")")]
                            ])
                         available-tokens))
     [:label "Token Amount"]
     [:input {:type :text :value @token-amount :on-change #(re/dispatch [:page.dev.contract-ops/set-token-amount (-> % .-target .-value)])}]
     [:input {:type :button :on-click #(re/dispatch [:page.dev.contract-ops/send-mint-token-tx]) :value "Mint!"}]
     [:p "Minted token with ID: "]
     [:code @minted-token-id]]))

(defmethod page :route.dev/contract-ops []
  (fn []
    [:div
     [:h1 "Contract operations"]
     [:h2 {:style {:font-size "3em"}}"Mint tokens"]
     [c-mint-tokens]
     [c-app-db]]))
