(ns ethlance.db
  (:require [cljs-web3.core :as web3]
            [ethlance.utils :as u]
            [re-frame.core :refer [dispatch]]
            [cljs-time.core :as t]
            [ethlance.constants :as constants]))

(def default-db
  {:web3 (web3/create-web3 "http://192.168.0.16:8545/")
   :active-page (u/match-current-location)
   :provides-web3? (boolean (or (aget js/window "web3") goog.DEBUG))
   :contracts-not-found? false
   :window/width-size (u/get-window-width-size js/window.innerWidth)
   :drawer-open? false
   :snackbar {:open? false
              :message ""
              :auto-hide-duration 5000
              :on-request-close #(dispatch [:snackbar/close])}
   :eth/config {:max-user-languages 10
                :min-user-languages 1
                :max-freelancer-categories (dec (count constants/categories))
                :min-freelancer-categories 0
                :max-freelancer-skills 10
                :min-freelancer-skills 0
                :max-job-skills 7
                :min-job-skills 1
                :max-user-description 1000
                :max-job-description 1000
                :min-job-description 0 #_100
                :max-invoice-description 500
                :max-feedback 1000
                :min-feedback 0 #_50
                :max-job-title 100
                :min-job-title 0 #_10
                :max-user-name 40
                :min-user-name 0 #_5
                :max-freelancer-job-title 50
                :min-freelancer-job-title 4
                :max-contract-desc 500
                :max-proposal-desc 500
                :max-invitation-desc 500
                :max-skills-create-at-once 50 #_10
                :adding-skills-enabled? 1}
   :eth/contracts {:ethlance-user {:name "EthlanceUser" :setter? true  :address "0x5743af1089c230e35c2ae14223dc372a32c0c60e"}
                   :ethlance-job {:name "EthlanceJob" :setter? true  :address "0xc383897837e24bcaf2645da740e217fe3ee33ec5"}
                   :ethlance-contract {:name "EthlanceContract" :setter? true :address "0x1bbe454a6620773d4e7652b648baf68830a4b521"}
                   :ethlance-invoice {:name "EthlanceInvoice" :setter? true :address "0xeded68900fd4aad8fbe20ee8b343200f9f709eb8"}
                   :ethlance-config {:name "EthlanceConfig" :setter? true :address "0x6864552b95434d806262b32b276178264ced0fa5"}
                   :ethlance-db {:name "EthlanceDB" :address "0xc56e67106a5824862fe138c04ccdfb49fa1d024c"}
                   :ethlance-views {:name "EthlanceViews" :address "0x6688b03fd1fba2be67c92a38c4e89675c7f9b575"}
                   :ethlance-search {:name "EthlanceSearch" :address "0x02452b3a90192d9928cd1d2061e52a7c3606677d"}}
   :my-addresses []
   :active-address nil
   :my-users-loaded? false
   :blockchain/addresses {}
   :app/users {}
   :app/jobs {}
   :app/contracts {}
   :app/invoices {}
   :app/skills {}

   :list/my-users {:items [] :loading? true :params {}}
   :list/contract-invoices {:items [] :loading? true :params {} :offset 0 :limit 4 :sort-dir :desc}
   :list/job-proposals {:items [] :loading? true :params {} :offset 0 :limit 4 :sort-dir :asc}
   :list/job-feedbacks {:items [] :loading? true :params {} :offset 0 :initial-limit 1 :limit 1 :show-more-limit 2 :sort-dir :desc}
   :list/job-invoices {:items [] :loading? true :params {} :offset 0 :limit 4 :sort-dir :desc}
   :list/employer-invoices-pending {:items [] :loading? true :params {} :offset 0 :limit 4 :sort-dir :desc}
   :list/employer-invoices-paid {:items [] :loading? true :params {} :offset 0 :limit 4 :sort-dir :desc}
   :list/freelancer-invoices-pending {:items [] :loading? true :params {} :offset 0 :limit 4 :sort-dir :desc}
   :list/freelancer-invoices-paid {:items [] :loading? true :params {} :offset 0 :limit 4 :sort-dir :desc}
   :list/search-freelancers {:items [] :loading? true :params {} :offset 0 :limit 3}
   :list/search-jobs {:items [] :loading? true :params {} :offset 0 :limit 10}
   :list/freelancer-feedbacks {:items [] :loading? true :params {} :offset 0 :initial-limit 1 :limit 1 :show-more-limit 2 :sort-dir :desc}
   :list/employer-feedbacks {:items [] :loading? true :params {} :offset 0 :initial-limit 1 :limit 1 :show-more-limit 2 :sort-dir :desc}
   :list/freelancer-invitations {:items [] :loading? true :params {} :offset 0 :limit 4 :sort-dir :desc}
   :list/freelancer-proposals {:items [] :loading? true :params {} :offset 0 :limit 4 :sort-dir :desc}
   :list/freelancer-contracts {:items [] :loading? true :params {} :offset 0 :limit 4 :sort-dir :desc}
   :list/freelancer-contracts-open {:items [] :loading? true :params {} :offset 0 :limit 4 :sort-dir :desc}
   :list/freelancer-contracts-done {:items [] :loading? true :params {} :offset 0 :limit 4 :sort-dir :desc}
   :list/employer-jobs-open {:items [] :loading? true :params {} :offset 0 :limit 4 :sort-dir :desc}
   :list/employer-jobs-done {:items [] :loading? true :params {} :offset 0 :limit 4 :sort-dir :desc}
   :list/employer-jobs {:items [] :loading? true :params {} :offset 0 :limit 4 :sort-dir :desc}
   :list/freelancer-my-open-contracts {:items [] :loading? true :params {}}
   :list/employer-jobs-open-select-field {:items [] :loading? false :params {}}

   :form.invoice/pay-invoice {:loading? false :gas-limit 200000}
   :form.invoice/cancel-invoice {:loading? false :gas-limit 200000}
   :form.job/set-hiring-done {:loading? false :gas-limit 200000}
   :form.job/add-job {:loading? false
                      :gas-limit 2000000
                      :data {:job/title ""
                             :job/description ""
                             :job/skills []
                             :job/language 40
                             :job/budget 0
                             :job/category 0
                             :job/payment-type 1
                             :job/experience-level 1
                             :job/estimated-duration 1
                             :job/hours-per-week 1
                             :job/freelancers-needed 1}
                      :errors #{:job/title :job/description :job/skills :job/category}}
   :form.contract/add-invitation {:loading? false
                                  :gas-limit 700000
                                  :data {:invitation/description ""
                                         :contract/job 0}
                                  :errors #{:contract/job}}

   :form.contract/add-proposal {:loading? false
                                :gas-limit 700000
                                :data {:proposal/description ""
                                       :proposal/rate 0}
                                :errors #{:proposal/description}}

   :form.contract/add-contract {:loading? false
                                :gas-limit 700000
                                :data {:contract/description ""
                                       :contract/hiring-done? false}
                                :errors #{}}

   :form.contract/add-feedback {:loading? false
                                :gas-limit 700000
                                :data {:contract/feedback ""
                                       :contract/feedback-rating 0}
                                :errors #{:contract/feedback}}

   :form.invoice/add-invoice {:loading? false
                              :gas-limit 700000
                              :data {:invoice/contract nil
                                     :invoice/description ""
                                     :invoice/amount 0
                                     :invoice/worked-hours 0
                                     :invoice/worked-from (u/timestamp-js->sol (u/get-time (u/week-ago)))
                                     :invoice/worked-to (u/timestamp-js->sol (u/get-time (t/today-at-midnight)))}
                              :errors #{:invoice/contract}}

   :form.config/add-skills {:loading? false
                            :gas-limit 4500000
                            :data {:skill/names []}
                            :errors #{:skill/names}}

   :form.user/set-user {:loading? false
                        :gas-limit 500000
                        :data {}
                        :errors #{}}

   :form.user/set-freelancer {:loading? false
                              :gas-limit 1000000
                              :data {}
                              :errors #{}
                              :open? false}

   :form.user/set-employer {:loading? false
                            :gas-limit 1000000
                            :data {}
                            :errors #{}
                            :open? false}

   :form.user/register-freelancer {:loading? false
                                   :gas-limit 2000000
                                   :open? true
                                   :data {:user/name ""
                                          :user/gravatar ""
                                          :user/country 0
                                          :user/languages [40]
                                          :freelancer/available? true
                                          :freelancer/job-title ""
                                          :freelancer/hourly-rate (web3/to-wei 1 :ether)
                                          :freelancer/categories []
                                          :freelancer/skills []
                                          :freelancer/description ""}
                                   :errors #{} #_#{:user/name #_:user/gravatar :user/country
                                                   :freelancer/job-title :freelancer/categories :freelancer/skills
                                                   :freelancer/description}}

   :form.user/register-employer {:loading? false
                                 :gas-limit 2000000
                                 :open? true
                                 :data {:user/name ""
                                        :user/gravatar ""
                                        :user/country 0
                                        :user/languages [40]
                                        :employer/description ""}
                                 :errors #{} #_#{:user/name #_:user/gravatar :user/country
                                                 :employer/description}}



   :form/search-jobs {:search/category 0
                      :search/skills []
                      :search/payment-types [1 2]
                      :search/experience-levels [1 2 3]
                      :search/estimated-durations [1 2 3 4]
                      :search/hours-per-weeks [1 2]
                      :search/min-budget 0
                      :search/min-employer-avg-rating 0
                      :search/min-employer-ratings-count 0
                      :search/country 0
                      :search/language 0
                      :search/offset 0
                      :search/limit 10}

   :form/search-freelancers {:search/category 0
                             :search/skills []
                             :search/min-avg-rating 0
                             :search/min-freelancer-ratings-count 0
                             :search/min-hourly-rate 0
                             :search/max-hourly-rate 0
                             :search/country 0
                             :search/language 0
                             :search/offset 0
                             :search/limit 3}
   }
  )
