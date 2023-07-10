(ns ethlance.ui.page.me.events
  (:require
   [ethlance.ui.event.utils :as event.utils]
   [district.ui.router.effects :as router.effects]
   [re-frame.core :as re]))

;;
;; Page State
;;

(def state-key :page.me)
(def state-default
  {:pagination-limit 15
   :pagination-offset 0})

(def create-assoc-handler (partial event.utils/create-assoc-handler state-key))

(defn initialize-page
  "Event FX Handler. Setup listener to dispatch an event when the page is active/visited."
  [{:keys [db]} _]
  (let [page-state (get db state-key)]
    {:db (assoc-in db [state-key] state-default)}))

;;
;; Page Events
;;


(re/reg-event-fx :page.me/initialize-page initialize-page)
(re/reg-event-fx :page.me/set-pagination-offset (create-assoc-handler :pagination-offset))
