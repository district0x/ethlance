(ns ethlance.server.model.candidate
  "Functions to get and set the candidate data for user's described by their user-id."
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


(s/def ::candidate-data
  (s/keys
   :req [:user/id
         :candidate/biography
         :candidate/date-registered
         :candidate/professional-title]))


(s/fdef register!
  :args (s/cat :candidate-data ::candidate-data))

(defn register!
  "Registering a user as a candidate"
  [candidate-data]
  (ethlance.db/insert-row! :UserCandidate candidate-data))


(s/fdef is-registered?
  :args (s/cat :user-id :user/id)
  :ret boolean?)

(defn is-registered?
  "Returns true if the given user with the given `user-id` is a
  registered candidate."
  [user-id]
  (-> (district.db/get {:select [1] :from [:UserCandidate] :where [:= :user/id user-id]})
      seq boolean))


(s/fdef category-listing
  :args (s/cat :user-id :user/id)
  :ret :candidate/categories)

(defn category-listing
  "Lists candidate's categories he is looking for employment in."
  [user-id]
  (if-let [listing (ethlance.db/get-list :UserCandidateCategory {:user/id user-id})]
    (mapv :category/name listing)
    []))


(s/fdef update-category-listing!
  :args (s/cat :user-id :user/id :listing :candidate/categories))

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


(s/fdef skill-listing
  :args (s/cat :user-id :user/id)
  :ret :candidate/skills)

(defn skill-listing
  "Lists candidate's set of skills."
  [user-id]
  (if-let [listing (ethlance.db/get-list :UserCandidateSkill {:user/id user-id})]
    (map :skill/name listing)
    []))


(s/fdef update-skill-listing!
  :args (s/cat :user-id :user/id :listing :candidate/skills))

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


(s/fdef get-data
  :args (s/cat :user-id :user/id)
  :ret ::candidate-data)

(defn get-data
  "Flat data making up the candidate."
  [user-id]
  (ethlance.db/get-row :UserCandidate {:user/id user-id}))

