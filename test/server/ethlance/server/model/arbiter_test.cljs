(ns ethlance.server.model.arbiter-test
  "Unit tests for the arbiter model."
  (:require
   [clojure.test :refer [deftest is are testing]]
   [bignumber.core :as bn]
   [cuerdas.core :as str]
   [district.server.config]
   [district.server.db :as district.db]
   [taoensso.timbre :as log]
   [ethlance.server.db :as ethlance.db]
   [ethlance.server.model.user :as user]
   [ethlance.server.model.arbiter :as arbiter]
   [ethlance.server.test-utils.db :refer [deftest-database] :include-macros true]
   [ethlance.shared.enum.payment-type :as enum.payment]
   [ethlance.shared.enum.currency-type :as enum.currency]))


(deftest-database main-arbiter-model {}
  (user/register! {:user/id 1
                   :user/address "0x1"
                   :user/country-code "CA"
                   :user/email "john.doe@gmail.com"
                   :user/profile-image ""
                   :user/date-last-active 0
                   :user/date-joined 0})
  (is (not (arbiter/is-registered? 1)))

  (testing "Registering an arbiter"
    (arbiter/register! {:user/id 1
                        :arbiter/biography "I am testy."
                        :arbiter/date-registered 0
                        :arbiter/currency-type ::enum.currency/eth
                        :arbiter/payment-value 5
                        :arbiter/payment-type ::enum.payment/percentage}))
  
  (is (arbiter/is-registered? 1)))

