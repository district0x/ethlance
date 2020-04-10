(ns ethlance.ui.event.job-listing
  (:require
   [re-frame.core :as re]))


(def state-key :ethlance.job-listing)
(def state-default
  {:max-per-page 10
   :data []})


(def mock-data
  [:test :test2])


(defn query
  "Event FX Handler. Perform Job Listing Query."
  [{:keys [db] :as cofxs} [_ {:keys [] :as opts}]]
  ;;TODO: mock up + production graphql
  {:dispatch [:job-listing/-set-data mock-data]})


(defn set-data
  "Event FX Handler. Set the Current Job Listing."
  [{:keys [db]} [_ data]]
  {:db (assoc-in db [state-key :data] data)})


;;
;; Registered Events
;;

(re/reg-event-fx :job-listing/query query)


;; Intermediates
(re/reg-event-fx :job-listing/-set-data set-data)
