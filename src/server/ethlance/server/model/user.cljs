(ns ethlance.server.model.user
  (:refer-clojure :exclude [exists?])
  (:require
   [bignumber.core :as bn]
   [cuerdas.core :as str]
   [district.server.config]
   [district.server.db :as district.db]
   [taoensso.timbre :as log]
   [ethlance.server.db :as ethlance.db]
   [ethlance.shared.spec :as espec]))


;; TODO: clojure.spec


(defn register!
  "Register a user with the given `user-data`."
  [user-data]
  (ethlance.db/insert-row! :User user-data))


(defn exists?
  "Returns true if the given user with the given id exists."
  [id]
  (let [q {:select [1] :from [:User] :where [:= id :user/id]}]
    (-> (district.db/get q) seq boolean)))


(defn get-data
  "Get the flat user data for user with `id`"
  [id]
  (ethlance.db/get-row :User {:user/id id}))


