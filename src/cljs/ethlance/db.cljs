(ns ethlance.db
  (:require [cljs-web3.core :as web3]
            [ethlance.utils :as u]))

(def default-db
  {:web3 (web3/create-web3 "http://localhost:8545/")
   :active-page (u/match-current-location)
   :provides-web3? (boolean (or (aget js/window "web3") goog.DEBUG))
   :drawer-open? true
   :eth/config {:max-user-languages 10
                :max-freelancer-categories 20
                :max-freelancer-skills 15
                :max-job-skills 7
                :max-user-description 1000
                :max-job-description 1000
                :max-invoice-description 500
                :max-feedback 1000
                :max-job-title 100
                :max-user-name 40
                :max-freelancer-job-title 50}
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
   :app/skills {}

   :list/search-jobs {:items [] :loading? false}
   :list/search-freelancers {:items [] :loading? false}
   :list/job-contracts {:items [] :loading? false :params {} :offset 0 :limit 4}

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
