(ns ui.ethlance.ui.events
  (:require [ajax.core :as ajax]
            [day8.re-frame.http-fx]
            [district.ui.graphql.utils :as graphql-ui-utils]
            [district.graphql-utils :as graphql-utils]
            [district.ui.graphql.events :as graphql.events]
            [district.ui.logging.events :as logging]
            [district.ui.web3-accounts.queries :as account-queries]
            [ethlance.ui.config :as config]
            [re-frame.core :as re-frame :refer [reg-event-fx reg-event-db]]))


(reg-event-fx
 ::sign-data-and-sign-in
 (fn [{:keys [db] :as cofxs} _]
   (let [from (account-queries/active-account db)
         data-str "Sign in to Ethlance!"]
     {:web3/personal-sign {:web3 (:web3 db)
                           :data-str data-str
                           :from from
                           :on-success [::sign-in data-str]
                           :on-error [::error]}})))

(defn- parse-query [query]
  (:query-str (graphql-ui-utils/parse-query {:queries [query]}
                                            {:kw->gql-name graphql-utils/kw->gql-name})))

(reg-event-fx
 ::sign-in
 (fn [{:keys [db] :as cofxs} [_ data signed-data]]
   (let [mutation (str "mutation" (parse-query [:sign-in
                                                {:data-signature signed-data
                                                 :data data}]))
         url (get-in config/get-config [:graphql :url])]
     {:http-xhrio {:method          :post
                   :uri             url
                   :params          {:query mutation}
                   :timeout         8000
                   :response-format (ajax/json-response-format {:keywords? true})
                   :format          (ajax/json-request-format)
                   :on-success      [::signed-in]
                   :on-failure      [::logging/error "Error calling sendVerificationCode"]}})))

(reg-event-fx
 ::signed-in
 (fn [{:keys [db]} [_ token]]
   {:dispatch [::graphql.events/set-authorization-token token]}))

(comment
  (re-frame/dispatch [::sign-data-and-sign-in])
  )
