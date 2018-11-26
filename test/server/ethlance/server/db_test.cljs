(ns ethlance.server.db-test
  (:require
   [clojure.test :refer [deftest is are testing async]]

   [taoensso.timbre :as log]
   [cuerdas.core :as str]
   [mount.core :as mount]

   [district.server.config :refer [config]]
   [district.server.db]

   [ethlance.server.db :as db]
   [ethlance.server.core]))


(def test-config
  "Test configuration for the database."
  (-> ethlance.server.core/main-config
      (merge {:logging {:level "debug" :console? true}})
      (update :db merge {:opts {:memory true}})
      #_(update :db merge {:opts {:memory false}
                           :path "target/test_ethlance.db"})))


(defn fixture-start
  "Test Fixture Setup."
  []
  (-> (mount/with-args test-config)
      (mount/only
       [#'district.server.db/db
        #'ethlance.server.db/ethlance-db])
      mount/start))


(defn fixture-stop
  "Test Fixture Teardown."
  []
  (mount/stop))


(deftest test-database-user
  (fixture-start)
  ;;
  
  (testing "Check if users are missing")

  (testing "Inserting a user row.."
    (db/insert-row! :User {:user/id 1
                           :user/address "0x1"
                           :user/country-code "CA"
                           :user/email "john.doe@gmail.com"
                           :user/profile-image ""
                           :user/date-last-active 0
                           :user/date-joined 0}))

  (testing "Getting a user row.."
    (let [user (db/get-row :User {:user/id 1})]
      (is (= (:user/email user) "john.doe@gmail.com"))))

  (testing "Get user count..."
    (let [user-list (db/get-list :User {})]
      (is (= (count user-list) 1))))

  (testing "Inserting User Candidate row..."
    (db/insert-row! :UserCandidate {:user/id 1
                                    :candidate/biography "Work work work..."
                                    :candidate/date-registered 0
                                    :candidate/professional-title "Professional Worker"})
    ;; Attempt to insert the same row twice
    (is (thrown?
         js/Error
         (db/insert-row! :UserCandidate {:user/id 1
                                         :candidate/biography "Work work work..."
                                         :candidate/date-registered 0
                                         :candidate/professional-title "Professional Worker"}))))

  (testing "Inserting candidate for unknown foreign user key"
    (is (thrown?
         js/Error
         (db/insert-row! :UserCandidate {:user/id 2
                                         :candidate/biography "Work work work..."
                                         :candidate/date-registered 0
                                         :candidate/professional-title "Professional Worker"}))))

  (testing "Getting a user candidate row... changing that row"
    (let [user-candidate (db/get-row :UserCandidate {:user/id 1})]
      (is (= (:candidate/professional-title user-candidate) "Professional Worker"))

      ;; Change row attribute
      (db/update-row! :UserCandidate (assoc user-candidate :candidate/professional-title "Pro Wrestler")))

    ;; Check to see if the attribute changed in the database.
    (let [user-candidate (db/get-row :UserCandidate {:user/id 1})]
      (is (= (:candidate/professional-title user-candidate) "Pro Wrestler"))))

  (testing "Getting a user candidate row that doesn't exist"
    (let [user-candidate (db/get-row :UserCandidate {:user/id 2})]
      (is (nil? user-candidate))))

  (testing "Adding a user candidate categories"
    (db/insert-row! :UserCandidateCategory {:user/id 1
                                            :category/name "Software Development"}))

  (testing "Check if it has been added"
    (let [category (db/get-row :UserCandidateCategory {:user/id 1 :category/id 1})]
      (is (= (:category/name category) "Software Development"))))

  (testing "Get category listing"
    (let [category-listing (db/get-list :UserCandidateCategory {:user/id 1})]
      (is (= (count category-listing) 1))))

  (testing "Adding user candidate skills"
    (db/insert-row! :UserCandidateSkill {:user/id 1
                                         :skill/name "Clojure"}))

  (testing "Check if it has been added"
    (let [skill (db/get-row :UserCandidateSkill {:user/id 1 :skill/id 1})]
      (is (= (:skill/name skill) "Clojure"))))

  (testing "Get skill listing"
    (let [skill-listing (db/get-list :UserCandidateSkill {:user/id 1})]
      (is (= (count skill-listing) 1))))

  (testing "Add")
  ;;
  (fixture-stop))
