(ns ethlance.ui.event.sign-in
  (:require [ajax.core :as ajax]
            [district.ui.logging.events :as logging.events]
            [district.ui.web3-accounts.queries :as account-queries]
            [ethlance.ui.config :as config]
            [re-frame.core :as re]))

(defn sign-in
  "Event FX Handler. Perform a sign in with the active ethereum account.

  # Notes

  - This will attempt to 'sign' the `data-str` using the given active
  account. If the signed message is valid, the active ethereum account
  will be signed in by providing the session with a JWT Token."
  [{:keys [db]} _]
  (let [active-account (account-queries/active-account db)
        data-str "Sign in to Ethlance!"]
    {:web3/personal-sign
     {:web3 (:web3 db)
      :data-str data-str
      :from active-account
      :on-success [:user/-authenticate {:active-account active-account
                                                :data-str data-str}]
      :on-error [::logging.events/error "Error Signing with Active Ethereum Account."]}}))

(defn- parse-query [_])

(defn authenticate
  "Event FX Handler. Authenticate the sign in for the given active account."
  [_ [_ {:keys [active-account data-str]} data-signature]]
  (let [mutation-query
        (str "mutation" (parse-query [:sign-in
                                      {:data-signature data-signature
                                       :data data-str}]))
        graphql-url (get-in (config/get-config) [:graphql :url])]
    {:http-xhrio
     {:method          :post
      :uri             graphql-url
      :params          {:query mutation-query}
      :timeout         8000
      :response-format (ajax/json-response-format {:keywords? true})
      :format          (ajax/json-request-format)
      :on-success      [:user/-set-active-session active-account]
      :on-failure      [::logging.events/error "Error Performing Sign In Authentication."]}}))

(defn set-active-session
  "Event FX Handler. Give the currently active account proper
  authorities and associate the active account as 'signed in'."
  [{:keys [db]} [_ active-account token]]
  {:db (assoc db :user/active-account active-account)
   :dispatch [:graphql.events/set-authorization-token token]})

(defn sign-out
  [{:keys [db]}]
  {:db (assoc db :user/active-account nil)})

(re/reg-event-fx :user/sign-in sign-in)
(re/reg-event-fx :user/sign-out sign-out)

;; Intermediates
(re/reg-event-fx :user/-authenticate authenticate)
(re/reg-event-fx :user/-set-active-session set-active-session)

(comment
  (re/dispatch [:sign-in]))
