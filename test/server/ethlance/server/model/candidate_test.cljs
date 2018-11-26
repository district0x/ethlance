(ns ethlance.server.model.candidate-test
  "Unit tests for the candidate model."
  (:require
   [clojure.test :refer [deftest is are testing]]
   [bignumber.core :as bn]
   [cuerdas.core :as str]
   [district.server.config]
   [district.server.db :as district.db]
   [taoensso.timbre :as log]
   [ethlance.server.db :as ethlance.db]
   [ethlance.server.model.user :as user]
   [ethlance.server.model.candidate :as candidate]
   [ethlance.server.test-utils.db :refer [deftest-database] :include-macros true]))


(deftest-database main-candidate-model {}
  (user/register! {:user/id 1
                   :user/address "0x1"
                   :user/country-code "CA"
                   :user/email "john.doe@gmail.com"
                   :user/profile-image ""
                   :user/date-last-active 0
                   :user/date-joined 0})

  (is (not (candidate/is-registered? 1)))

  (testing "Registering a candidate"
    (candidate/register! {:user/id 1
                          :candidate/biography "A testy fellow"
                          :candidate/date-registered 0
                          :candidate/professional-title "Software Developer"})

    (is (candidate/is-registered? 1)))

  (testing "Getting main data"
    (let [candidate-data (candidate/get-data 1)]
      (log/debug candidate-data)
      (is (= (:candidate/biography candidate-data) "A testy fellow"))
      (is (= (:candidate/professional-title candidate-data) "Software Developer"))))

  (testing "Getting and Updating Category Listing"
    (is (= 0 (count (candidate/category-listing 1))))
    (candidate/update-category-listing! 1 ["Software Development" "Unit-Testing"])
    (is (= ["Software Development" "Unit-Testing"] (candidate/category-listing 1)))
    (candidate/update-category-listing! 1 ["Software Development" "Unit-Testing" "Woodworking"])
    (is (= ["Software Development" "Unit-Testing" "Woodworking"] (candidate/category-listing 1))))

  (testing "Getting and Updating Skill Listing"
    (is (= 0 (count (candidate/skill-listing 1))))
    (candidate/update-skill-listing! 1 ["Clojure" "Clojurescript"])
    (is (= ["Clojure" "Clojurescript"] (candidate/skill-listing 1)))
    (candidate/update-skill-listing! 1 ["Clojure" "Clojurescript" "Solidity"])
    (is (= ["Clojure" "Clojurescript" "Solidity"] (candidate/skill-listing 1)))))
