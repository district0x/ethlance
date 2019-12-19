(ns ethlance.ui.component.modal.events
  (:require
   [re-frame.core :as re]))


(defn open-modal!
  "Event FX Handler."
  [{:keys [db]} [_ modal-id]]
  {:db (assoc db :ethlance.ui.component.modal/active-modal-id (or modal-id :default))})


(defn close-modal!
  "Event FX Handler."
  [{:keys [db]} _]
  {:db (dissoc db :ethlance.ui.component.modal/active-modal-id)})


;;
;; Registered Events
;;


(re/reg-event-fx :modal/open open-modal!)
(re/reg-event-fx :modal/close close-modal!)
