(ns ethlance.ui.graphql
  (:require
   [district.shared.async-helpers :refer [promise->]]
   [re-frame.core :as re-frame]
   [taoensso.timbre :as log]
   [cljsjs.axios :as axios]
   [camel-snake-kebab.extras :as camel-snake-extras]
   [ethlance.ui.util.component :refer [>evt]]

   [district.graphql-utils :as graphql-utils]))

(defonce axios js/axios)

(defn gql->clj [m]
  (->> m
       (js->clj)
       (camel-snake-extras/transform-keys graphql-utils/gql-name->kw )))

(defmulti handler
  (fn [_ key value]
    (cond
      (:error value) :api/error
      (vector? key) (first key)
      :else key)))

(defn- update-db [cofx fx]
  (if-let [db (:db fx)]
    (assoc cofx :db db)
    cofx))

(defn- safe-merge [fx new-fx]
  (reduce (fn [merged-fx [k v]]
            (if (= :db k)
              (assoc merged-fx :db v)))
          fx
          new-fx))

(defn- do-reduce-handlers
  [{:keys [db] :as cofx} f coll]
  (reduce (fn [fxs element]
            (let [updated-cofx (update-db cofx fxs)]
              (if element
                (safe-merge fxs (f updated-cofx element))
                fxs)))
          {:db db}
          coll))

(defn reduce-handlers
  [cofx response]
  (do-reduce-handlers cofx
                      (fn [fxs [k v]]
                        ;; (log/debug "@ calling handler" {:k k :v v :fx fxs})
                        (handler fxs k v))
                      response))

(re-frame/reg-event-fx
 ::response
 (fn [{:keys [db] :as cofx} [_ response]]
   (reduce-handlers cofx response)))

(re-frame/reg-fx
 ::query
 (fn [[params callback]]
   (promise-> (axios params)
              callback)))

(re-frame/reg-event-fx
 ::query
 (fn [{:keys [db]} [_ {:keys [query variables on-success on-error]}]]
   (let [url (get-in db [:ethlance/config :graphql :url])
         access-token (get-in db [:tokens :access-token])
         params (clj->js {:url url
                          :method :post
                          :headers (merge {"Content-Type" "application/json"
                                           "Accept" "application/json"}
                                          (when access-token
                                            {"access_token" access-token}))
                          :data (js/JSON.stringify
                                 (clj->js {:query query
                                           :variables variables}))})
         callback (fn [^js response]
                    (if (= 200 (.-status response))
                      ;; TODO we can still have errors even with a 200
                      ;; so we should log them or handle in some other way
                      (>evt [::response (gql->clj (.-data (.-data response)))])
                      (log/error "Error during query" {:error (js->clj (.-data response) :keywordize-keys true)})))]
     {::query [params callback]})))

(defmethod handler :default
  [{:keys [db] :as cofx} k values]
  ;; NOTE: this is the default handler that is intented for queries and mutations
  ;; that have nothing to do besides reducing over their response values
  (log/debug "default handler" {:k k
                                :cofx cofx})
  (reduce-handlers cofx values))

(defmethod handler :user
  [{:keys [db] :as cofx} _ {:user/keys [address email]
                            :as user}]
  (log/debug "user handler" user)
  {:db (assoc-in db [:users address] user)})

(defmethod handler :candidate
  [{:keys [db] :as cofx} _ {:user/keys [address]
                            :candidate/keys [rate rate-currency-id]
                            :as candidate}]
  (log/debug "candidate handler" candidate)
  {:db (assoc-in db [:candidates address]
                 candidate)})

(defmethod handler :github-sign-up
  [{:keys [db] :as cofx} _ {:user/keys [address github-username] :as user}]
  (log/debug "github-sign-up handler" user)
  {:db (assoc-in db [:users address] (merge user
                                            {:user/user-name github-username}))})

(defmethod handler :update-candidate
  [{:keys [db] :as cofx} _ {user-date-registered :user/date-registered
                            candidate-date-registered :candidate/date-registered
                            address :user/address
                            :as candidate}]
  (log/debug "update-candidate handler" candidate)
  {:db (-> db
           (assoc-in [:users address :user/date-registered] user-date-registered)
           (assoc-in [:candidates address :candidate/date-registered] candidate-date-registered))})

(defmethod handler :api/error
  [_ _ _]
  ;; NOTE: this handler is here only to catch errors
  )
