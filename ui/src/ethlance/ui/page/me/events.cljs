(ns ethlance.ui.page.me.events
  (:require
    [ethlance.ui.event.utils :as event.utils]
    [re-frame.core :as re]))


;;
;; Page State
;;

(def state-key :page.me)


(def state-default
  {:pagination-limit 10
   :pagination-offset 0})


(def create-assoc-handler (partial event.utils/create-assoc-handler state-key))


(defn initialize-page
  "Event FX Handler. Setup listener to dispatch an event when the page is active/visited."
  [{:keys [db]} _]
  {:db (assoc db state-key state-default)})


;;
;; Page Events
;;


(re/reg-event-fx :page.me/initialize-page initialize-page)
(re/reg-event-fx :page.me/set-pagination-offset (create-assoc-handler :pagination-offset))
