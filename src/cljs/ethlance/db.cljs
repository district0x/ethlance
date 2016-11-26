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
   :drawer-open? true}
  )
