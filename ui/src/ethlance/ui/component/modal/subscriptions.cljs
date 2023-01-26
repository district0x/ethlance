(ns ethlance.ui.component.modal.subscriptions
  (:require
   [re-frame.core :as re]))


(re/reg-sub
 :modal/open?
 (fn [db [_ modal-id]]
   (let [active-modal-id (:ethlance.ui.component.modal/active-modal-id db)]
     (= active-modal-id (or modal-id :default)))))

