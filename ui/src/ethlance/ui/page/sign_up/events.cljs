(ns ethlance.ui.page.sign-up.events
  (:require
    [district.parsers :as parsers]
    [district.ui.logging.events :as logging]
    [district.ui.web3-accounts.events :as accounts-events]
    [district.ui.web3-accounts.queries :as accounts-queries]
    [ethlance.ui.event.utils :as event.utils]
    [district.ui.graphql.events :as gql-events]
    [ethlance.ui.util.component :refer [>evt]]
    [re-frame.core :as re]))

(def state-key :page.sign-up)
(def state-default
  {:candidate/rate-currency-id :USD
   :arbiter/fee-currency-id :USD})

(def create-assoc-handler (partial event.utils/create-assoc-handler state-key))

(re/reg-event-fx :page.sign-up/set-user-name (create-assoc-handler :user/name))
(re/reg-event-fx :page.sign-up/set-user-email (create-assoc-handler :user/email))
(re/reg-event-fx :page.sign-up/set-user-country-code (create-assoc-handler :user/country))
(re/reg-event-fx :page.sign-up/set-user-languages (create-assoc-handler :user/languages))
(re/reg-event-fx :page.sign-up/set-user-profile-image (create-assoc-handler :user/profile-image))
(re/reg-event-fx :page.sign-up/set-candidate-professional-title (create-assoc-handler :candidate/professional-title))
(re/reg-event-fx :page.sign-up/set-candidate-rate (create-assoc-handler :candidate/rate))
(re/reg-event-fx :page.sign-up/set-candidate-categories (create-assoc-handler :candidate/categories))
(re/reg-event-fx :page.sign-up/set-candidate-skills (create-assoc-handler :candidate/skills))
(re/reg-event-fx :page.sign-up/set-candidate-bio (create-assoc-handler :candidate/bio))
(re/reg-event-fx :page.sign-up/set-employer-professional-title (create-assoc-handler :employer/professional-title))
(re/reg-event-fx :page.sign-up/set-employer-bio (create-assoc-handler :employer/bio))
(re/reg-event-fx :page.sign-up/set-arbiter-fee (create-assoc-handler :arbiter/fee))
(re/reg-event-fx :page.sign-up/set-arbiter-professional-title (create-assoc-handler :arbiter/professional-title))
(re/reg-event-fx :page.sign-up/set-arbiter-bio (create-assoc-handler :arbiter/bio))


(def interceptors [re/trim-v])

(re/reg-event-fx
  :page.sign-up/initialize-page
  (fn [{:keys [db]}]
    {:db (assoc-in db [state-key] state-default)
     ; :forward-events
     ; {:register ::accounts-loaded?
     ;  :events #{::accounts-events/accounts-changed}
     ;  :dispatch-to [:page.sign-up/initial-query]}
     }))


(re/reg-event-fx
  ::unregister-initial-query-forwarder
  (fn []
    {:forward-events {:unregister ::initial-query?}}))

(defn- fallback-data [db section address]
  (merge (get-in db [:users address]) (get-in db [section address])))

(def user-fields
  [:user/id
   :user/email
   :user/country
   :user/name
   :user/languages
   :user/profile-image])

(def candidate-fields
  [:candidate/professional-title
   :candidate/rate
   :candidate/categories
   :candidate/bio
   :candidate/skills
   :candidate/rate-currency-id])

(def employer-fields
  [:employer/professional-title
   :employer/bio])

(def arbiter-fields
  [:arbiter/professional-title
   :arbiter/bio
   :arbiter/fee
   :arbiter/fee-currency-id])

(defn remove-nil-vals-from-map [input-map]
  (reduce (fn [acc [k v]]
            (if (nil? v)
              acc
              (assoc acc k v)))
          {}
          input-map))

(re/reg-event-fx
  :page.sign-up/update-candidate
  [interceptors]
  (fn [{:keys [db]} [form]]
    (let [user-address (accounts-queries/active-account db)
          set->vec (fn [v] (if (set? v) (vec v) v))
          user-params (update-vals
                        (remove-nil-vals-from-map (select-keys form user-fields))
                        set->vec)
          candidate-params (update-vals
                             (remove-nil-vals-from-map (select-keys form candidate-fields))
                             set->vec)
          query [:update-user
                 {:user/id user-address :user user-params :candidate candidate-params}
                 [:user/id]]]
      {:dispatch [::gql-events/mutation
                  {:queries [query]
                   :on-success [:navigate-to-profile user-address "candidate"]}]})))

(re/reg-event-fx
  :page.sign-up/update-employer
  [interceptors]
  (fn [{:keys [db]} [form]]
    (let [user-address (accounts-queries/active-account db)
          set->vec (fn [v] (if (set? v) (vec v) v))
          user-params (update-vals
                        (remove-nil-vals-from-map (select-keys form user-fields))
                        set->vec)
          employer-params (remove-nil-vals-from-map (select-keys form employer-fields))
          query [:update-user
                 {:user/id user-address :user user-params :employer employer-params}
                 [:user/id]]]
      {:dispatch [::gql-events/mutation
                  {:queries [query]
                   :on-success [:navigate-to-profile user-address "employer"]}]})))

(re/reg-event-fx
  :page.sign-up/update-arbiter
  [interceptors]
  (fn [{:keys [db]} [form]]
    (let [user-address (accounts-queries/active-account db)
          set->vec (fn [v] (if (set? v) (vec v) v))
          user-params (update-vals
                        (remove-nil-vals-from-map (select-keys form user-fields))
                        set->vec)
          arbiter-params (remove-nil-vals-from-map (select-keys form arbiter-fields))
          query [:update-user
                 {:user/id user-address :user user-params :arbiter arbiter-params}
                 [:user/id]]]
      {:dispatch [::gql-events/mutation
                  {:queries [query]
                   :on-success [:navigate-to-profile user-address "arbiter"]}]})))

(re/reg-event-fx
  :navigate-to-profile
  (fn [cofx [_ address tab]]
    {:fx [[:dispatch [:district.ui.router.events/navigate :route.user/profile {:address address} {:tab tab}]]
          [:dispatch [:district.ui.user-profile-updated]]]}))

(re/reg-event-fx
  :page.sign-up/upload-user-image
  [interceptors]
  (fn [_ [{:keys [:file-info] :as data}]]
    {:ipfs/call {:func "add"
                 :args [(:file file-info)]
                 :on-success [::upload-user-image-success data]
                 :on-error [::logging/error "Error uploading user image" {:data data}]}}))

(re/reg-event-fx
  ::upload-user-image-success
  [interceptors]
  (fn [_ [_ ipfs-resp]]
    {:dispatch [:page.sign-up/set-user-profile-image (:Hash ipfs-resp)]}))

