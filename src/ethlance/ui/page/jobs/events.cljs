(ns ethlance.ui.page.jobs.events
  (:require
   [re-frame.core :as re]

   [ethlance.shared.mock :as mock]))


(def state-key :page.jobs)
(def state-default
  {:job-listing {:max-per-page 10 :data [] :loading? true :initialzed? false}})


(defonce mock-job-listing
  (mapv mock/generate-mock-job (range 1 10)))


(defn query-job-listing
  "Event FX Handler. Perform Job Listing Query."
  [{:keys [db] :as cofxs} [_ {:keys [] :as opts}]]
  ;;TODO: mock up + production graphql
  {:dispatch [:page.jobs/-set-job-listing mock-job-listing]})


(defn set-job-listing
  "Event FX Handler. Set the Current Job Listing."
  [{:keys [db]} [_ data]]
  {:db (assoc-in db [state-key :data] data)})


;;
;; Registered Events
;;

;; TODO: switch based on dev environment
(re/reg-event-fx :page.jobs/query-job-listing query-job-listing)


;; Intermediates
(re/reg-event-fx :page.jobs/-set-job-listing set-job-listing)


