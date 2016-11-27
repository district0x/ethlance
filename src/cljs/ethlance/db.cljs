(ns ethlance.db
  (:require [cljs-web3.core :as web3]
            [ethlance.utils :as u]))
;
;setConfig("max-user-languages", 10);
;setConfig("max-freelancer-categories", 20);
;setConfig("max-freelancer-skills", 15);
;setConfig("max-job-skills", 7);
;setConfig("max-user-description", 1000);
;setConfig("max-job-description", 1000);
;setConfig("max-invoice-description", 500);
;setConfig("max-feedback", 1000);
;setConfig("max-job-title", 100);

(def default-db
  {:web3 (web3/create-web3 "http://localhost:8545/")
   :active-page (u/match-current-location)
   :provides-web3? (boolean (or (aget js/window "web3") goog.DEBUG))
   :drawer-open? true
   :ethlance-config-values {:max-user-languages 10
                            :max-freelancer-categories 20
                            :max-freelancer-skills 15
                            :max-job-skills 7
                            :max-user-description 1000
                            :max-job-description 1000
                            :max-invoice-description 500
                            :max-feedback 1000
                            :max-job-title 100}
   :contracts {:ethlance-user {:name "EthlanceUser" :setter? true}
               :ethlance-job {:name "EthlanceJob" :setter? true}
               :ethlance-contract {:name "EthlanceContract" :setter? true}
               :ethlance-invoice {:name "EthlanceInvoice" :setter? true}
               :ethlance-config {:name "EthlanceConfig" :setter? true}
               :ethlance-db {:name "EthlanceDB"}
               :ethlance-views {:name "EthlanceViews"}
               :ethlance-search {:name "EthlanceSearch"}}
   }
  )
