(ns ethlance.db
  (:require [cljs-web3.core :as web3]
            [ethlance.utils :as u]
            [re-frame.core :refer [dispatch]]
            [cljs-time.core :as t]))

(def default-db
  {:web3 (web3/create-web3 "http://localhost:8545/")
   :active-page (u/match-current-location)
   :provides-web3? (boolean (or (aget js/window "web3") goog.DEBUG))
   :drawer-open? true
   :snackbar {:open? false
              :message ""
              :auto-hide-duration 5000
              :on-request-close #(dispatch [:snackbar/close])}
   :eth/config {:max-user-languages 10
                :max-freelancer-categories 20
                :max-freelancer-skills 15
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
                :max-contract-desc 500
                :max-proposal-desc 500
                :max-invitation-desc 500
                :max-skills-create-at-once 10
                :adding-skills-enabled? 1}
   :eth/contracts {:ethlance-user {:name "EthlanceUser" :setter? true}
                   :ethlance-job {:name "EthlanceJob" :setter? true}
                   :ethlance-contract {:name "EthlanceContract" :setter? true}
                   :ethlance-invoice {:name "EthlanceInvoice" :setter? true}
                   :ethlance-config {:name "EthlanceConfig" :setter? true}
                   :ethlance-db {:name "EthlanceDB"}
                   :ethlance-views {:name "EthlanceViews"}
                   :ethlance-search {:name "EthlanceSearch"}}
   :my-addresses []
   :active-address nil
   :address->user-id {}
   :app/users {}
   :app/jobs {}
   :app/contracts {}
   :app/invoices {}
   :app/skills {}

   :list/contract-invoices {:items [] :loading? true :params {} :offset 0 :limit 4 :sort-dir :desc}
   :list/job-proposals {:items [] :loading? true :params {} :offset 0 :limit 4}
   :list/job-feedbacks {:items [] :loading? true :params {} :offset 0 :initial-limit 1 :limit 1 :show-more-limit 2 :sort-dir :desc}
   :list/job-invoices {:items [] :loading? true :params {} :offset 0 :limit 4 :sort-dir :desc}
   :list/employer-invoices-pending {:items [] :loading? true :params {} :offset 0 :limit 4 :sort-dir :desc}
   :list/employer-invoices-paid {:items [] :loading? true :params {} :offset 0 :limit 4 :sort-dir :desc}
   :list/freelancer-invoices-pending {:items [] :loading? true :params {} :offset 0 :limit 4 :sort-dir :desc}
   :list/freelancer-invoices-paid {:items [] :loading? true :params {} :offset 0 :limit 4 :sort-dir :desc}
   :list/search-freelancers {:items [] :loading? true :params {} :offset 0 :limit 10}
   :list/search-jobs {:items [] :loading? true :params {} :offset 0 :limit 10}
   :list/user-feedbacks {:items [] :loading? true :params {} :offset 0 :limit 4 :sort-dir :desc}
   :list/freelancer-invitations {:items [] :loading? true :params {} :offset 0 :limit 4 :sort-dir :desc}
   :list/freelancer-proposals {:items [] :loading? true :params {} :offset 0 :limit 4 :sort-dir :desc}
   :list/freelancer-contracts-open {:items [] :loading? true :params {} :offset 0 :limit 4 :sort-dir :desc}
   :list/freelancer-contracts-done {:items [] :loading? true :params {} :offset 0 :limit 4 :sort-dir :desc}
   :list/employer-jobs-open {:items [] :loading? true :params {} :offset 0 :limit 4 :sort-dir :desc}
   :list/employer-jobs-done {:items [] :loading? true :params {} :offset 0 :limit 4 :sort-dir :desc}
   :list/freelancer-my-open-contracts {:items [] :loading? true :params {}}

   :form.invoice/pay {:loading? false :gas-limit 200000}
   :form.invoice/cancel {:loading? false :gas-limit 200000}
   :form.job/set-hiring-done {:loading? false :gas-limit 200000}
   :form.job/add-job {:loading? false
                      :gas-limit 700000
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
                            :gas-limit 700000
                            :data {:skill/names []}
                            :errors #{:skill/names}}

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
                             :search/limit 10}
   }
  )
