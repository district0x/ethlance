(ns ethlance.ui.event.sign-in
  (:refer-clojure :exclude [resolve])
  (:require
    [district.ui.graphql.events :as gql-events]
    [district.ui.logging.events :as logging.events]
    [district.ui.web3-accounts.queries :as account-queries]
    [district.ui.web3.queries :as web3-queries]
    [re-frame.core :as re]))


(re/reg-event-fx
  :user/sign-in
  ;; Event FX Handler. Perform a sign in with the active ethereum account.
  ;;
  ;; # Notes
  ;;
  ;; - This will attempt to 'sign' the `data-str` using the given active
  ;; account. If the signed message is valid, the active ethereum account
  ;; will be signed in by providing the session with a JWT Token.
  (fn [{:keys [db]} _]
    (let [active-account (account-queries/active-account db)
          data-str " Sign in to Ethlance! "]
      {:web3/personal-sign
       {:web3 (web3-queries/web3 db)
        :data-str data-str
        :from active-account
        :on-success [:user/-authenticate {:data-str data-str}]
        :on-error [::logging.events/error " Error Signing with Active Ethereum Account. "]}})))


(re/reg-event-fx
  :user/sign-out
  ;; TODO Remove JWT server-side
  [(re/inject-cofx :store)]
  (fn [{:keys [db store]}]
    {:db (dissoc db :active-session)
     :store (dissoc store :active-session)}))


(re/reg-event-fx
  ::store-active-session
  (fn [cofx [_ event-data]]
    (-> cofx
        (assoc-in ,,, [:db :active-session] (select-keys (:sign-in event-data) [:jwt :user/id]))
        (assoc-in ,,, [:store] (select-keys (:sign-in event-data) [:jwt :user/id]))
        (assoc-in ,,, [:fx] [[:dispatch [:district.ui.graphql.events/set-authorization-token (get-in event-data [:sign-in :jwt])]]]))))


;; Intermediates
(re/reg-event-fx
  ;; Event FX Handler. Authenticate the sign in for the given active account.
  :user/-authenticate
  (fn [_ [_ {:keys [data-str]} data-signature]]
    {:dispatch [::gql-events/mutation
                {:queries [[:sign-in {:data-signature data-signature
                                      :data data-str}
                            [:jwt :user/id]]]
                 :on-success [::store-active-session]}]}))


(comment
  (re/dispatch [:user/sign-in]))
