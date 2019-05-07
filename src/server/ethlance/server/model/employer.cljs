(ns ethlance.server.model.employer
  "Functions to get and set the employer data for user's described by
  their user-id."
  (:require
   [clojure.spec.alpha :as s]
   [bignumber.core :as bn]
   [cuerdas.core :as str]
   [district.server.config]
   [district.server.db :as district.db]
   [taoensso.timbre :as log]
   [ethlance.server.db :as ethlance.db]
   [ethlance.shared.enumeration.currency-type :as enum.currency]
   [ethlance.shared.enumeration.payment-type :as enum.payment]

   ;; Includes additional spec namespaces
   [ethlance.shared.spec :as espec]))


(defn- enum-kw->val
  "Replace employer data's keyword enumerations into their respective
  values."
  [m]
  (-> m))


(defn- enum-val->kw
  [m]
  (-> m))


(s/def ::employer-data
  (s/keys
   :req [:user/id
         :employer/biography
         :employer/date-registered
         :employer/professional-title]))


(s/fdef register!
   :args (s/cat :employer-data ::employer-data))

(defn register!
  "Registering a user as an employer"
  [employer-data]
  (let [employer-data (enum-kw->val employer-data)]
    (ethlance.db/insert-row! :UserEmployer employer-data)))


(s/fdef is-registered?
  :args (s/cat :user-id :user/id)
  :ret boolean?)

(defn is-registered?
  "Returns true if the given user with the given `user-id` is a
  registered employer."
  [user-id]
  (-> (district.db/get {:select [1] :from [:UserEmployer] :where [:= :user/id user-id]})
      seq boolean))


(s/fdef get-data
  :args (s/cat :user-id :user/id)
  :ret (s/nilable ::employer-data))

(defn get-data [user-id]
  (let [employer (ethlance.db/get-row :UserEmployer {:user/id user-id})]
    (enum-val->kw employer)))
