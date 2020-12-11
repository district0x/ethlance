(ns ethlance.ui.graphql
  (:require
    ["axios" :as axios]
    [camel-snake-kebab.core :as camel-snake]
    [camel-snake-kebab.extras :as camel-snake-extras]
    [clojure.string :as string]
    [district.shared.async-helpers :refer [promise->]]
    [ethlance.ui.util.component :refer [>evt]]
    [re-frame.core :as re]
    [taoensso.timbre :as log]))

(defn gql-name->kw [gql-name]
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


(re/reg-fx
  ::query
  (fn [[params callback]]
    (promise-> (axios params)
               callback)))

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
                                             {"access-token" access-token}))
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
  [cofx k values]
  ;; NOTE: this is the default handler that is intented for queries and mutations
  ;; that have nothing to do besides reducing over their response values
  (log/debug "default handler" {:k k})
  (reduce-handlers cofx values))

(defmethod handler :user
  [{:keys [db]} _ {:user/keys [address] :as user}]
  (log/debug "user handler" user)
  {:db (assoc-in db [:users address] user)})

(defmethod handler :candidate
  [{:keys [db]} _ {:user/keys [address] :as candidate}]
  (log/debug "candidate handler" candidate)
  {:db (assoc-in db [:candidates address] candidate)})

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
  [_ _ _]
  ;; NOTE: this handler is here only to catch errors
  )
