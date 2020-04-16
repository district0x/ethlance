(ns ethlance.ui.event.job-listing
  (:require
   [re-frame.core :as re]
   
   [ethlance.shared.mock :as mock]))


(def state-key :ethlance.job-listing)
(def state-default
  {:max-per-page 10
   :data []})


(defonce mock-job-listing
  (mapv mock/generate-mock-job (range 1 10)))


(defn mock-query
  "Event FX Handler. Perform Job Listing Query."
  [{:keys [db] :as cofxs} [_ {:keys [] :as opts}]]
  ;;TODO: mock up + production graphql
  {:dispatch [:job-listing/-set-data mock-job-listing]})


(defn set-data
  "Event FX Handler. Set the Current Job Listing."
  [{:keys [db]} [_ data]]
  {:db (assoc-in db [state-key :data] data)})


;;
;; Registered Events
;;

;; TODO: switch based on dev environment
(re/reg-event-fx :job-listing/query mock-query)


;; Intermediates
(re/reg-event-fx :job-listing/-set-data set-data)
