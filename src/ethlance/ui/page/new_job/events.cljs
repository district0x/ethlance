(ns ethlance.ui.page.new-job.events
  (:require
   [re-frame.core :as re]
   [district.parsers :refer [parse-int parse-float]]
   [district.ui.router.effects :as router.effects]
   [district.ui.web3.queries :as web3-queries]
   [ethlance.shared.constants :as constants]
   [ethlance.shared.mock :as mock]
   [ethlance.ui.event.utils :as event.utils]
   [ethlance.ui.event.templates :as event.templates]))

;; Page State
(def state-key :page.new-job)
(def state-default
  {:type :job
   :name nil
   :category nil
   :bid-option :hourly-rate
   :required-experience-level :intermediate
   :estimated-project-length :day
   :required-availability :full-time
   :required-skills #{}
   :description nil
   :form-of-payment :ethereum
   :token-address nil
   :with-arbiter? true})


(defn initialize-page
  "Event FX Handler. Setup listener to dispatch an event when the page is active/visited."
  [{:keys [db]} _]
  (let [page-state (get db state-key)]
    {::router.effects/watch-active-page
     [{:id :page.new-job/initialize-page
       :name :route.job/new
       :dispatch []}]}))


;;
;; Registered Events
;;
(def create-assoc-handler (partial event.utils/create-assoc-handler state-key))


;; TODO: switch based on dev environment
(re/reg-event-fx :page.new-job/initialize-page initialize-page)
(re/reg-event-fx :page.new-job/set-type (create-assoc-handler :type))
(re/reg-event-fx :page.new-job/set-name (create-assoc-handler :name))
(re/reg-event-fx :page.new-job/set-category (create-assoc-handler :category))
(re/reg-event-fx :page.new-job/set-bid-option (create-assoc-handler :bid-option))
(re/reg-event-fx :page.new-job/set-required-experience-level (create-assoc-handler :required-experience-level))
(re/reg-event-fx :page.new-job/set-estimated-project-length (create-assoc-handler :estimated-project-length))
(re/reg-event-fx :page.new-job/set-required-availability (create-assoc-handler :required-availability))
(re/reg-event-fx :page.new-job/set-required-skills (create-assoc-handler :required-skills))
(re/reg-event-fx :page.new-job/set-description (create-assoc-handler :description))
(re/reg-event-fx :page.new-job/set-form-of-payment (create-assoc-handler :form-of-payment))
(re/reg-event-fx :page.new-job/set-token-address (fn [{:keys [db]} [_ token-address]]
                                                   {:db (assoc-in db [state-key :token-address] token-address)
                                                    :web3.erc20/fetch-token-symbol {:token-address token-address
                                                                                    :web3 (web3-queries/web3 db)}}))
(re/reg-event-fx :page.new-job/set-token-symbol (create-assoc-handler :token-symbol))
(re/reg-event-fx :page.new-job/set-with-arbiter? (create-assoc-handler :with-arbiter?))
;; w
