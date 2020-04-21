(ns ethlance.ui.page.jobs.events
  (:require
   [re-frame.core :as re]
   [district.ui.router.effects :as router.effects]
   [ethlance.shared.mock :as mock]))


(def state-key :page.jobs)
(def state-default
  {:job-listing/max-per-page 10
   :job-listing/state :start
   :job-listing []})


(defonce mock-job-listing
  (mapv mock/generate-mock-job (range 1 10)))


(defn initialize-page
  "Event FX Handler. Setup listener to dispatch an event when the page is active."
  [{:keys [db]} _]
  (let [page-state (-> db state-key)]
    {::router.effects/watch-active-page
     [{:id :page.jobs/initialize-page
       :name :route.job/jobs
       :dispatch [:page.jobs/query-job-listing {}]}]}))


(defn query-job-listing
  "Event FX Handler. Perform Job Listing Query."
  [{:keys [db] :as cofxs} [_ {:keys [] :as opts}]]
  ;;TODO: mock up + production graphql
  (let [job-listing mock-job-listing]
    {:db (assoc-in db [state-key :job-listing/state] :loading)
     :dispatch [:page.jobs/-set-job-listing job-listing]}))


(defn set-job-listing
  "Event FX Handler. Set the Current Job Listing."
  [{:keys [db]} [_ job-listing]]
  {:db (-> db
           (assoc-in [state-key :job-listing/state] :done)
           (assoc-in [state-key :job-listing] job-listing))})


;;
;; Registered Events
;;

;; TODO: switch based on dev environment
(re/reg-event-fx :page.jobs/initialize-page initialize-page)
(re/reg-event-fx :page.jobs/query-job-listing query-job-listing)


;; Intermediates
(re/reg-event-fx :page.jobs/-set-job-listing set-job-listing)


