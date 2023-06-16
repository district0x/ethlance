(ns ethlance.ui.page.me.events
  (:require

   [district.ui.router.effects :as router.effects]
   [re-frame.core :as re]))

;;
;; Page State
;;

(def state-key :page.me)
(def state-default
  {})

(defn initialize-page
  "Event FX Handler. Setup listener to dispatch an event when the page is active/visited."
  [{:keys [db]} _]
  (println ">>> initializing :page.me")
  (let [page-state (get db state-key)]
    {:db (assoc-in db [state-key] state-default)}))

;;
;; Page Events
;;


(re/reg-event-fx :page.me/initialize-page initialize-page)
