(ns ethlance.server.contract.ethlance-user-test
  "Unit Tests for EthlanceUser wrapper."
  (:require
   [bignumber.core :as bn]
   [clojure.test :refer [deftest is are testing use-fixtures]]
   [cljs-web3.eth :as web3-eth]
   [taoensso.timbre :as log]

   [district.server.web3 :refer [web3]]
   [district.server.smart-contracts :as contracts]

   [ethlance.server.contract.ethlance-user :as user :include-macros true]
   [ethlance.server.contract.ethlance-user-factory :as user-factory]
   [ethlance.server.contract.ethlance-registry :as registry]
   [ethlance.server.contract.ds-guard :as ds-guard]
   [ethlance.server.test-utils :refer-macros [deftest-smart-contract]]))


(def sample-meta-hash-1 "QmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJH")
(def sample-meta-hash-2 "JmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJ9")


(deftest-smart-contract register-and-retrieve-user {}
  (let [[user1] (web3-eth/accounts @web3)
        tx-1 (user-factory/register-user!
              {:metahash-ipfs sample-meta-hash-1}
              {:from user1})]
    
    (testing "Check if metahash is correct"
      (user/with-ethlance-user (user-factory/user-by-address user1)
        (is (= sample-meta-hash-1 (user/metahash-ipfs)))))))


(deftest-smart-contract attempt-reconstruction {}
  (let [[user1 user2] (web3-eth/accounts @web3)
        tx-1 (user-factory/register-user!
              {:metahash-ipfs sample-meta-hash-1}
              {:from user1})]
    (testing "Attempt to reconstruct user from unprivileged account"
      ;; Only the factory is privileged to construct the user contract
      (is (thrown?
           js/Error
           (user/with-ethlance-user (user-factory/user-by-address user1)
             (contracts/contract-call
              user/*user-key* :construct user2 sample-meta-hash-2
              {:from user1}))))

      ;; Other users shouldn't be able to construct it.
      (is (thrown?
           js/Error
           (user/with-ethlance-user (user-factory/user-by-address user1)
             (contracts/contract-call
              user/*user-key* :construct user2 sample-meta-hash-2
              {:from user2})))))))


(deftest-smart-contract update-user-metahash {}
  (let [[user1 user2] (web3-eth/accounts @web3)
        tx-1 (user-factory/register-user!
              {:metahash-ipfs sample-meta-hash-1}
              {:from user1})]

   (testing "Update the user metahash"
     (user/with-ethlance-user (user-factory/user-by-address user1)
       (is (= (user/metahash-ipfs) sample-meta-hash-1))
       (user/update-metahash! sample-meta-hash-2 {:from user1})
       (is (= (user/metahash-ipfs) sample-meta-hash-2))))

   (testing "User can't update another users contract"
     (user/with-ethlance-user (user-factory/user-by-address user1)
       (is (thrown? js/Error (user/update-metahash! sample-meta-hash-1 {:from user2})))))))


(deftest-smart-contract register-candidate {}
  (let [[user1 user2] (web3-eth/accounts @web3)
        tx-1 (user-factory/register-user!
              {:metahash-ipfs sample-meta-hash-1}
              {:from user1})

        tx-2 (user-factory/register-user!
              {:metahash-ipfs sample-meta-hash-2}
              {:from user2})]

    (testing "Try and register a candidate for different user"
      (user/with-ethlance-user (user-factory/user-by-address user1)
        (is (thrown? js/Error
                     (user/register-candidate!
                      {:hourly-rate 99
                       :currency-type 1} ;; USD
                      {:from user2})))))

    (testing "Register as a candidate"
      (user/with-ethlance-user (user-factory/user-by-address user1)
        (user/register-candidate!
         {:hourly-rate 100
          :currency-type 1} ;; USD
         {:from user1})))

    (testing "Try and register candidate twice"
      (user/with-ethlance-user (user-factory/user-by-address user1)
        (is (thrown? js/Error 
                     (user/register-candidate!
                      {:hourly-rate 100
                       :currency-type 1} ;; USD
                      {:from user1})))))

    (testing "Get the candidate data"
      (user/with-ethlance-user (user-factory/user-by-address user1)
        (let [candidate-data (user/candidate-data)]
          (is (:is-registered? candidate-data))
          (is (bn/= (:hourly-rate candidate-data) 100))
          (is (bn/= (:currency-type candidate-data) 1)))))

    (testing "Update registered candidate"
      (user/with-ethlance-user (user-factory/user-by-address user1)
        (user/update-candidate!
         {:hourly-rate 80
          :currency-type 0}
         {:from user1})
        (let [candidate-data (user/candidate-data)]
          (is (:is-registered? candidate-data))
          (is (bn/= (:hourly-rate candidate-data) 80))
          (is (bn/= (:currency-type candidate-data) 0)))))

    (testing "Try and update candidate as other user"
      (user/with-ethlance-user (user-factory/user-by-address user1)
        (is (thrown? js/Error
                     (user/update-candidate!
                      {:hourly-rate 80
                       :currency-type 0}
                      {:from user2})))))))


(deftest-smart-contract register-arbiter {}
  (let [[user1 user2] (web3-eth/accounts @web3)
        tx-1 (user-factory/register-user!
              {:metahash-ipfs sample-meta-hash-1}
              {:from user1})

        tx-2 (user-factory/register-user!
              {:metahash-ipfs sample-meta-hash-2}
              {:from user2})]

    (testing "Try and register an arbiter for different user"
      (user/with-ethlance-user (user-factory/user-by-address user1)
        (is (thrown? js/Error
                     (user/register-arbiter!
                      {:payment-value 99
                       :currency-type 1 ;; USD
                       :type-of-payment 0}
                      {:from user2})))))

    (testing "Register as an Arbiter"
      (user/with-ethlance-user (user-factory/user-by-address user1)
        (user/register-arbiter!
         {:payment-value 100
          :currency-type 1 ;; USD
          :type-of-payment 0} ;; Fixed
         {:from user1})))

    (testing "Try and register arbiter twice"
      (user/with-ethlance-user (user-factory/user-by-address user1)
        (is (thrown? js/Error 
                     (user/register-arbiter!
                      {:payment-value 100
                       :currency-type 1 ;; USD
                       :type-of-payment 0} ;; USD
                      {:from user1})))))

    (testing "Get the arbiter data"
      (user/with-ethlance-user (user-factory/user-by-address user1)
        (let [arbiter-data (user/arbiter-data)]
          (is (:is-registered? arbiter-data))
          (is (bn/= (:payment-value arbiter-data) 100))
          (is (bn/= (:currency-type arbiter-data) 1))
          (is (bn/= (:type-of-payment arbiter-data) 0)))))

    (testing "Update registered arbiter"
      (user/with-ethlance-user (user-factory/user-by-address user1)
        (user/update-arbiter!
         {:payment-value 3
          :currency-type 0 ;; ETH
          :type-of-payment 1} ;; Percent
         {:from user1})
        (let [arbiter-data (user/arbiter-data)]
          (is (:is-registered? arbiter-data))
          (is (bn/= (:currency-type arbiter-data) 0))
          (is (bn/= (:payment-value arbiter-data) 3))
          (is (bn/= (:type-of-payment arbiter-data) 1)))))

    (testing "Try and update arbiter as other user"
      (user/with-ethlance-user (user-factory/user-by-address user1)
        (is (thrown? js/Error
                     (user/update-arbiter!
                      {:payment-value 100
                       :currency-type 1 ;; USD
                       :type-of-payment 0}
                      {:from user2})))))))


(deftest-smart-contract register-employer {}
  (let [[user1 user2] (web3-eth/accounts @web3)
        tx-1 (user-factory/register-user!
              {:metahash-ipfs sample-meta-hash-1}
              {:from user1})

        tx-2 (user-factory/register-user!
              {:metahash-ipfs sample-meta-hash-2}
              {:from user2})]

    (testing "Attempt to register employer for other user"
      (user/with-ethlance-user (user-factory/user-by-address user1)
        (is (thrown? js/Error (user/register-employer! {:from user2})))))

    (testing "Register an employer"
      (user/with-ethlance-user (user-factory/user-by-address user1)
        (user/register-employer! {:from user1})))

    (testing "Attempt to register twice"
      (user/with-ethlance-user (user-factory/user-by-address user1)
        (is (thrown? js/Error (user/register-employer! {:from user1})))))

    (testing "Get employer data"
      (user/with-ethlance-user (user-factory/user-by-address user1)
        (let [employer-data (user/employer-data)]
          (is (:is-registered? employer-data)))))))
