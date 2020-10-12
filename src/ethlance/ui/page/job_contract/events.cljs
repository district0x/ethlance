(ns ethlance.ui.page.job-contract.events
  (:require [district.ui.router.effects :as router.effects]
            [ethlance.ui.event.utils :as event.utils]
            [re-frame.core :as re]))

;; Page State
(def state-key :page.job-contract)
(def state-default
  {})

(defn initialize-page
  "Event FX Handler. Setup listener to dispatch an event when the page is active/visited."
  []
  {::router.effects/watch-active-page
   [{:id :page.job-contract/initialize-page
     :name :route.job/contract
     :dispatch []}]})

;;
;; Registered Events
;;
(def create-assoc-handler (partial event.utils/create-assoc-handler state-key))


;; TODO: switch based on dev environment
(re/reg-event-fx :page.job-contract/initialize-page initialize-page)
