(ns ethlance.ui.subscription.utils)


(defn create-get-handler
  [state-key key]
  (fn [db _]
    (get-in db [state-key key])))
