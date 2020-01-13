(ns ethlance.server.graphql.resolvers-test
  (:require [cljs.core.async :refer [go <!]]
            [cljs.nodejs :as nodejs]
            [cljs.test :refer-macros [deftest is testing async use-fixtures]]
            [district.server.db :as db]
            [district.server.logging]
            [district.shared.async-helpers :as async-helpers :refer [promise->]]
            [ethlance.server.db]
            [ethlance.server.graphql.generator :as generator]
            [ethlance.server.graphql.server]
            [ethlance.server.graphql.utils :refer [run-query]]
            [mount.core :as mount]
            [taoensso.timbre :as log]))

(async-helpers/extend-promises-as-channels!)

(use-fixtures :once
  {:before (fn []
             (log/debug "Running before fixture")
             (-> (mount/with-args {:db {:opts {:memory true}}
                                   :ethlance/db {:resync? false}
                                   :graphql {:port 4000}
                                   :logging {:level :debug
                                             :console? true}})
                 (mount/only [#'district.server.logging/logging
                              #'district.server.db/db
                              #'ethlance.server.db/ethlance-db
                              #'ethlance.server.graphql.server/graphql])
                 (mount/start)
                 (as-> $ (log/warn "Started" $))))
   :after (fn [] (log/debug "Running after fixture"))})

(deftest test-resolvers
  (async done
         (go
           (let [api-endpoint "http://localhost:4000/graphql"
                 _ (<! (generator/generate-dev-data))
                 {{{:keys [:user/address]} :user} :data
                  :as user-query} (<! (run-query {:url api-endpoint
                                                  :query [:user {:user/address "EMPLOYER"}
                                                          [:user/address]]}))

                 user-search-query (<! (run-query {:url api-endpoint
                                                   :query [:user-search {:offset 0 :limit 3}
                                                           [:total-count
                                                            [:items [:user/address]]]]}))

                 candidate-query (<! (run-query {:url api-endpoint
                                                 :query [:candidate {:user/address "CANDIDATE"}
                                                         [:user/address
                                                          [:candidate/feedback [:total-count
                                                                                [:items
                                                                                 [:job/id
                                                                                  :contract/id
                                                                                  :feedback/to-user-type
                                                                                  :feedback/from-user-type
                                                                                  :feedback/rating]]]]]]}))

                 candidate-search-query-or (<! (run-query {:url api-endpoint
                                                           :query [:candidate-search {:skills-or ["Clojure" "Travis"]}
                                                                   [:total-count
                                                                    [:items [:user/address
                                                                             :candidate/skills]]]]}))

                 candidate-search-query-and (<! (run-query {:url api-endpoint
                                                            :query [:candidate-search {:skills-and ["Clojure" "Java"]}
                                                                    [:total-count
                                                                     [:items [:user/address
                                                                              :candidate/skills]]]]}))

                 employer-query (<! (run-query {:url api-endpoint
                                                :query [:employer {:user/address "EMPLOYER"}
                                                        [:user/address
                                                         [:employer/feedback [:total-count
                                                                              [:items
                                                                               [:job/id
                                                                                :contract/id
                                                                                :feedback/to-user-type
                                                                                :feedback/from-user-type
                                                                                :feedback/rating]]]]]]}))

                 arbiter-query (<! (run-query {:url api-endpoint
                                               :query [:arbiter {:user/address "ARBITER"}
                                                       [:user/address
                                                        [:arbiter/feedback [:total-count
                                                                            [:items
                                                                             [:job/id
                                                                              :contract/id
                                                                              :feedback/to-user-type
                                                                              :feedback/from-user-type
                                                                              :feedback/rating]]]]]]}))

                 job-id (-> candidate-query :data :candidate :candidate/feedback :items first :job/id)
                 job-query (<! (run-query {:url api-endpoint
                                           :query [:job {:job/id job-id}
                                                   [:job/id
                                                    :job/accepted-arbiter-address
                                                    :job/employer-address
                                                    [:job/contracts [:total-count
                                                                     [:items
                                                                      [:job/id
                                                                       :contract/id
                                                                       [:contract/employer-feedback [:job/id
                                                                                                     :contract/id
                                                                                                     :feedback/rating
                                                                                                     :feedback/to-user-type
                                                                                                     :feedback/from-user-type]]
                                                                       [:contract/candidate-feedback [:job/id
                                                                                                      :contract/id
                                                                                                      :feedback/rating
                                                                                                      :feedback/to-user-type
                                                                                                      :feedback/from-user-type]]]]]]]]}))

                 contract-query (<! (run-query {:url api-endpoint
                                                :query [:contract {:contract/id 0 :job/id job-id}
                                                        [:job/id
                                                         :contract/id
                                                         [:contract/disputes [:total-count
                                                                              [:items
                                                                               [:job/id
                                                                                :contract/id
                                                                                :dispute/reason]]]]
                                                         [:contract/invoices [:total-count
                                                                              [:items
                                                                               [:job/id
                                                                                :contract/id
                                                                                :invoice/id
                                                                                :invoice/amount-paid]]]]]]}))

                 dispute-query (<! (run-query {:url api-endpoint
                                                :query [:dispute {:contract/id 0 :job/id job-id}
                                                        [:job/id
                                                         :contract/id
                                                         :dispute/reason]]}))

                 invoice-id (-> contract-query :data :contract :contract/invoices :items first :invoice/id)
                 invoice-query (<! (run-query {:url api-endpoint
                                               :query [:invoice {:contract/id 0 :job/id job-id :invoice/id (or invoice-id 0)}
                                                       [:job/id
                                                        :contract/id
                                                        :invoice/id
                                                        :invoice/amount-paid]]}))]

             (is (= "EMPLOYER" (-> user-query :data :user :user/address)))

             (is (= 3 (-> user-search-query :data :user-search :total-count)))
             (is (= 3 (-> user-search-query :data :user-search :items count)))

             (is (= "CANDIDATE" (-> candidate-query :data :candidate :user/address)))
             (is (= 5 (-> candidate-query :data :candidate :candidate/feedback :total-count)))
             (is (= 5 (-> candidate-query :data :candidate :candidate/feedback :items count)))
             (is (= "Employer" (-> candidate-query :data :candidate :candidate/feedback :items first :feedback/from-user-type)))
             (is (= "Candidate" (-> candidate-query :data :candidate :candidate/feedback :items first :feedback/to-user-type)))

             (is (= ["Clojure" "Solidity"] (-> candidate-search-query-or :data :candidate-search :items first :candidate/skills)))
             (is (= 0 (-> candidate-search-query-and :data :candidate-search :total-count)))

             (is (every? #(= "Employer" %) (-> employer-query :data :employer :employer/feedback :items (#(map :feedback/to-user-type %)) )))

             (is (every? #(= "Arbiter" %) (-> arbiter-query :data :employer :arbiter/feedback :items (#(map :feedback/to-user-type %)) )))

             (let [employer-feedbacks (-> employer-query :data :employer :employer/feedback :items)
                   employer-feedback (-> job-query :data :job :job/contracts :items first :contract/employer-feedback)
                   candidate-feedbacks (-> candidate-query :data :candidate :candidate/feedback :items)
                   candidate-feedback (-> job-query :data :job :job/contracts :items first :contract/candidate-feedback)]

               (is (= (-> (filter (fn [elem] (and (= job-id (:job/id elem))
                                                  (= (:contract/id employer-feedback) (:contract/id elem))))
                                  employer-feedbacks) first)
                      (-> job-query :data :job :job/contracts :items first :contract/employer-feedback)))

               (is (= (-> job-query :data :job :job/contracts :items first :contract/candidate-feedback)
                      (-> (filter (fn [elem] (and (= job-id (:job/id elem))
                                                  (= (:contract/id candidate-feedback) (:contract/id elem))))
                                  candidate-feedbacks) first))))

             (let [contract-invoices (-> contract-query :data :contract :contract/invoices :items)
                   contract-disputes (-> contract-query :data :contract :contract/disputes :items)]

               (is (= (-> invoice-query :data :invoice)
                      (first (filter (fn [elem] (and (= job-id (:job/id elem))
                                                     (= 0 (:contract/id elem))
                                                     (= invoice-id (:invoice/id elem))))
                                     contract-invoices))))

               (when-not (empty? contract-disputes)
                 (is (= (-> dispute-query :data :dispute)
                        (first (filter (fn [elem] (and (= job-id (:job/id elem))
                                                       (= 0 (:contract/id elem))))
                                       contract-disputes))))))

             (done)))))
