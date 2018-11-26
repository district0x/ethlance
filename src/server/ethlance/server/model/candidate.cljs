(ns ethlance.server.model.candidate
  "Functions to get and set the candidate data for user's described by their user-id."
  (:require
   [bignumber.core :as bn]
   [cuerdas.core :as str]
   [district.server.config]
   [district.server.db :as district.db]
   [taoensso.timbre :as log]
   [ethlance.server.db :as ethlance.db]))


;; TODO: clojure.spec


(defn register!
  "Registering a user as a candidate"
  [candidate-data]
  (ethlance.db/insert-row! :UserCandidate candidate-data))


(defn is-registered?
  "Returns true if the given user with the given `user-id` is a
  registered candidate."
  [user-id]
  (-> (district.db/get {:select [1] :from [:UserCandidate] :where [:= :user/id user-id]})
      seq boolean))


(defn category-listing
  "Lists candidate's categories he is looking for employment in."
  [user-id]
  (if-let [listing (ethlance.db/get-list :UserCandidateCategory {:user/id user-id})]
    (mapv :category/name listing)
    []))


(defn update-category-listing!
  "Clear out and replace the candidate category listing with the updated
  category `listing`, which is a sequence of category names.

  Notes:

  - Data comes from immutable IPFS structures containing all categories in the listing."
  [user-id listing]

  ;; clear the old data
  (district.db/run! {:delete-from :UserCandidateCategory
                     :where [:= :user/id user-id]})
  
  ;; populate with new data
  (doseq [name listing]
    (ethlance.db/insert-row! :UserCandidateCategory {:user/id user-id :category/name name})))


(defn skill-listing
  "Lists candidate's set of skills."
  [user-id]
  (if-let [listing (ethlance.db/get-list :UserCandidateSkill {:user/id user-id})]
    (map :skill/name listing)
    []))


(defn update-skill-listing!
  "Clear out and replace the candidate skill listing with the updated
  skill `listing`, which is a sequence of skill names.

  Notes:

  - Data comes from immutable IPFS structures containing all categories in the listing."
  [user-id listing]

  ;; clear the old data
  (district.db/run! {:delete-from :UserCandidateSkill
                     :where [:= :user/id user-id]})
  
  ;; populate with new data
  (doseq [name listing]
    (ethlance.db/insert-row! :UserCandidateSkill {:user/id user-id :skill/name name})))


(defn get-data
  "Flat data making up the candidate."
  [user-id]
  (ethlance.db/get-row :UserCandidate {:user/id user-id}))

