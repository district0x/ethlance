(ns ethlance.ui.page.new-invoice.events
  (:require [district.parsers :refer [parse-float parse-int]]
            [district.ui.router.effects :as router.effects]
            [ethlance.ui.event.utils :as event.utils]
            [re-frame.core :as re]))

(def state-key :page.new-invoice)

(def state-default
  {:job-name-listing ["Smart Contract" "USD" "ETH"]
   :job-name nil
   :hours-worked nil
   :hourly-rate nil
   :invoice-amount nil
   :message nil})

(def create-assoc-handler (partial event.utils/create-assoc-handler state-key))

(defn initialize-page
  "Event FX Handler. Setup listener to dispatch an event when the page is active/visited."
  []
  {::router.effects/watch-active-page
   [{:id :page.new-invoice/initialize-page
     :name :route.invoice/new
     :dispatch []}]})

(re/reg-event-fx :page.new-invoice/initialize-page initialize-page)
(re/reg-event-fx :page.new-invoice/set-job-name-listing (create-assoc-handler :job-name-listing))
(re/reg-event-fx :page.new-invoice/set-job-name (create-assoc-handler :job-name))
(re/reg-event-fx :page.new-invoice/set-hours-worked (create-assoc-handler :hours-worked parse-int))
(re/reg-event-fx :page.new-invoice/set-hourly-rate (create-assoc-handler :hourly-rate parse-float))
(re/reg-event-fx :page.new-invoice/set-invoice-amount (create-assoc-handler :invoice-amount parse-float))
(re/reg-event-fx :page.new-invoice/set-message (create-assoc-handler :message))
