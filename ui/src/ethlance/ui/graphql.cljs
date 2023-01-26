(ns ethlance.ui.graphql
  (:require
    ["axios" :as axios]
    [camel-snake-kebab.core :as camel-snake]
    [camel-snake-kebab.extras :as camel-snake-extras]
    [clojure.string :as string]
    [ethlance.ui.util.component :refer [>evt]]
    [re-frame.core :as re]
    [district.ui.notification.events :as events]
    [taoensso.timbre :as log]))

(defn gql-name->kw
  "Turns names from graphql <namespace>_<camelCasedName> into <namespace>/<kebab-cased-name>
     Example: user_isRegisteredCandidate -> user/is-registered-candidate"
  [gql-name]
  (when gql-name
    (let [k (name gql-name)]
      (if (string/starts-with? k "__")
        (keyword k)
        (let [k (if (string/ends-with? k "_")
                  (str (.slice k 0 -1) "?")
                  k)
              parts (string/split k "_")
              parts (if (< 2 (count parts))
                      [(string/join "." (butlast parts)) (last parts)]
                      parts)]
          (apply keyword (map camel-snake/->kebab-case parts)))))))

(defn gql->clj [m]
  (->> m
    (js->clj)
    (camel-snake-extras/transform-keys gql-name->kw)))

(defmulti handler
          (fn [_ key value]
            (cond
              (:error value) :api/error
              (vector? key) (first key)
              :else key)))


(defn- update-db [cofx fx]
  (cond-> cofx
    (:db fx) (assoc :db (:db fx))
    (:store fx) (assoc :store (:store fx))))


(defn- safe-merge [fx new-fx]
  (reduce (fn [merged-fx [k v]]
            (cond
              (= :db k) (assoc merged-fx :db v)
              (= :store k) (assoc merged-fx :store v)
              :else nil))
          fx
          new-fx))


(defn- do-reduce-handlers
  [{:keys [db store] :as cofx} f coll]
  (reduce (fn [fxs element]
            (let [updated-cofx (update-db cofx fxs)]
              (if element
                (safe-merge fxs (f updated-cofx element))
                fxs)))
          {:db db
           :store store}
          coll))

(defn reduce-handlers
  [cofx response]
  (do-reduce-handlers cofx
                      (fn [fxs [k v]]
                        (handler fxs k v))
                      response))

(re/reg-event-fx
  ::response
  [(re/inject-cofx :store)]
  (fn [cofx [_ response]]
    (reduce-handlers cofx response)))

(re/reg-event-db
  ::request-finished
  (fn [db _]
    (assoc db :api-request-in-progress false)))

(re/reg-event-db
  ::request-started
  (fn [db _]
    (assoc db :api-request-in-progress true)))

(re/reg-fx
  ::query
  (fn [[params callback]]
    (>evt [::request-started])
    (-> (axios params)
        (.then callback)
        (.finally #(>evt [::request-finished])))))

(re/reg-event-fx
  ::query
  (fn [{:keys [db]} [_ {:keys [query variables]}]]
    (let [url (get-in db [:ethlance/config :graphql :url])
          access-token (get-in db [:active-session :jwt])
          params (clj->js {:url url
                           :method :post
                           :headers (merge {"Content-Type" "application/json"
                                            "Accept" "application/json"}
                                           (when access-token
                                             {"Authorization" (str "Bearer " access-token)}))
                           :data (js/JSON.stringify
                                   (clj->js {:query query
                                             :variables variables}))})
          callback (fn [^js response]
                     (if (= 200 (.-status response))
                       (do
                         (>evt [::response {:api/error (map :message (gql->clj (.-errors (.-data response))))}])
                         (doseq [message (map :message (gql->clj (.-errors (.-data response))))]
                           (>evt [::events/show {:message message}]))
                         (>evt [::response (gql->clj (.-data (.-data response)))]))
                       (log/error "Error during query" {:error (js->clj (.-data response) :keywordize-keys true)})))]
      {::query [params callback]})))

(defmethod handler :default
  [cofx k values]
  ;; NOTE: this is the default handler that is intented for queries and mutations
  ;; that have nothing to do besides reducing over their response values
  (log/debug "default handler" {:k k})
  (reduce-handlers cofx values))

(defmethod handler :user
  [{:keys [db]} _ {:user/keys [address] :as user}]
  (log/debug "user handler" user)
  {:db (assoc-in db [:users address] user)})

(defn- merge-in-changed-parts
  "Useful in cases when app-db key can get new data from
  different queries. E.g. get candidate generic fields like rate and bio
  from one and ratings from another. Leaves existing data intact, merging in new data"
  [db lookup new-data]
  (let [old-value (get-in db lookup {})
        new-value (merge old-value new-data)]
    (assoc-in db lookup new-value)))

(defmethod handler :candidate
  [{:keys [db]} _ {:user/keys [address] :as candidate}]
  (log/debug "candidate handler" candidate)
  {:db (merge-in-changed-parts db [:candidates address] candidate)})

(defmethod handler :employer
  [{:keys [db]} _ {:user/keys [address] :as employer}]
  (log/debug "employer handler" employer)
  {:db (assoc-in db [:employers address] employer)})

(defmethod handler :arbiter
  [{:keys [db]} _ {:user/keys [address] :as arbiter}]
  (log/debug "arbiter handler" arbiter)
  {:db (assoc-in db [:arbiters address] arbiter)})

(defmethod handler :github-sign-up
  [{:keys [db]} _ {:user/keys [address github-username] :as user}]
  (log/debug "github-sign-up handler" user)
  {:db (assoc-in db [:users address] (merge user
                                            {:user/name github-username}))})

(defmethod handler :linkedin-sign-up
  [{:keys [db]} _ {:user/keys [address name] :as user}]
  (log/debug "linkedin-sign-up handler" user)
  {:db (assoc-in db [:users address] (merge user {:user/name name}))})

(defmethod handler :update-candidate
  [{:keys [db]} _ {user-date-updated :user/date-updated
                   candidate-date-updated :candidate/date-updated
                   address :user/address
                   :as candidate}]
  (log/debug "update-candidate handler" candidate)
  {:db (-> db
         (assoc-in [:users address :user/date-updated] user-date-updated)
         (assoc-in [:candidates address :candidate/date-updated] candidate-date-updated))})

(defmethod handler :update-employer
  [{:keys [db]} _ {user-date-updated :user/date-updated
                   employer-date-updated :employer/date-updated
                   address :user/address
                   :as employer}]
  (log/debug "update-employer handler" employer)
  {:db (-> db
         (assoc-in [:users address :user/date-updated] user-date-updated)
         (assoc-in [:employers address :employer/date-updated] employer-date-updated))})

(defmethod handler :update-arbiter
  [{:keys [db]} _ {user-date-updated :user/date-updated
                   arbiter-date-updated :arbiter/date-updated
                   address :user/address
                   :as arbiter}]
  (log/debug "update-arbiter handler" arbiter)
  {:db (-> db
         (assoc-in [:users address :user/date-updated] user-date-updated)
         (assoc-in [:arbiters address :arbiter/date-updated] arbiter-date-updated))})

(defmethod handler :sign-in
  [{:keys [db store]} _ {:as response}]
  (log/debug "sign in handler " response)
  {:db (assoc db :active-session response)
   :store (assoc store :active-session response)})


(defmethod handler :api/error
  [{:keys [:db]} _ error-messages]
  (log/debug (str "api/error handler " error-messages))
  {:db (merge db {:api-errors error-messages})})
