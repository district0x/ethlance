(ns ethlance.ui.page.invoices.events
  (:require [district.ui.router.effects :as router.effects]
            [ethlance.ui.event.utils :as event.utils]
            [re-frame.core :as re]))

;; Page State
(def state-key :page.invoices)
(def state-default
  {})

(defn initialize-page
  "Event FX Handler. Setup listener to dispatch an event when the page is active/visited."
  [{:keys []} _]
  {::router.effects/watch-active-page
   [{:id :page.invoices/initialize-page
     :name :route.invoice/index
     :dispatch []}]})

;;
;; Registered Events
;;
(def create-assoc-handler (partial event.utils/create-assoc-handler state-key))


;; TODO: switch based on dev environment
(re/reg-event-fx :page.invoices/initialize-page initialize-page)
