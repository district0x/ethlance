(ns ethlance.server.model.arbiter
  "Functions to get and set the arbiter data for user's described by their user-id."
  (:require
   [bignumber.core :as bn]
   [cuerdas.core :as str]
   [district.server.config]
   [district.server.db :as district.db]
   [taoensso.timbre :as log]
   [ethlance.server.db :as ethlance.db]
   [ethlance.shared.enum.currency-type :as enum.currency]
   [ethlance.shared.enum.payment-type :as enum.payment]))


(defn register!
  "Registering a user as a arbiter"
  [arbiter-data]
  (ethlance.db/insert-row! :UserArbiter arbiter-data))


(defn is-registered?
  "Returns true if the given user with the given `user-id` is a
  registered arbiter."
  [user-id]
  (-> (district.db/get {:select [1] :from [:UserArbiter] :where [:= :user/id user-id]})
      seq boolean))
