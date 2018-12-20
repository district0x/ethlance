(ns ethlance.server.model.user
  (:refer-clojure :exclude [exists?])
  (:require
   [clojure.spec.alpha :as s]
   [bignumber.core :as bn]
   [cuerdas.core :as str]
   [district.server.config]
   [district.server.db :as district.db]
   [taoensso.timbre :as log]
   [ethlance.server.db :as ethlance.db]
   
   ;; Includes additional spec namespaces
   [ethlance.shared.spec :as espec]))


(s/def ::user-data
  (s/keys
   :req [:user/id
         :user/address
         :user/country-code
         :user/email
         :user/date-created
         :user/date-updated] 
   :opt [:user/full-name
         :user/user-name
         :user/profile-image]))


(s/fdef register!
  :args (s/cat :user-data ::user-data))

(defn register!
  "Register a user with the given `user-data`."
  [user-data]
  (ethlance.db/insert-row! :User user-data))


(s/fdef update!
  :args (s/cat :user-data (s/keys :req [:user/id])))

(defn update!
  [user-data]
  (ethlance.db/update-row! :User user-data))


(s/fdef exists?
  :args (s/cat :id :user/id)
  :ret boolean?)

(defn exists?
  "Returns true if the given user with the given id exists."
  [id]
  (let [q {:select [1] :from [:User] :where [:= id :user/id]}]
    (-> (district.db/get q) seq boolean)))


(s/fdef language-listing
  :args (s/cat :id :user/id)
  :ret :user/languages)

(defn language-listing
  [id]
  (let [listing (ethlance.db/get-list :UserLanguage {:user/id id})]
    (mapv :language/name listing)))


(s/fdef update-language-listing!
  :args (s/cat :id :user/id :listing :user/languages))

(defn update-language-listing!
  [id listing]

  ;; clear old data
  (district.db/run! {:delete-from :UserLanguage
                     :where [:= :user/id id]})

  ;; populate the new data
  (doseq [name listing]
    (ethlance.db/insert-row! :UserLanguage {:user/id id :language/name name})))


(defn get-data
  "Get the flat user data for user with `id`"
  [id]
  (ethlance.db/get-row :User {:user/id id}))


