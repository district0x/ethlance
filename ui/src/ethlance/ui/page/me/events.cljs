(ns ethlance.ui.page.me.events
  (:require
   [re-frame.core :as re]))


;;
;; Page State
;;


(def state-key :page.me)
(def state-default
  {:current-sidebar-choice :my-employer-job-listing})


;;
;; Page Events
;;


(re/reg-event-fx
 :page.me/change-sidebar-choice
 (fn [{:keys [db]} [_ location]]
   {:db (assoc-in db [state-key :current-sidebar-choice] location)}))
