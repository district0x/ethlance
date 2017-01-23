(ns ethlance.pages.employer-jobs-page
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.icons :as icons]
    [ethlance.components.jobs-table :refer [jobs-table]]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a center-layout]]
    [ethlance.ethlance-db :as ethlance-db]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [re-frame.core :refer [subscribe dispatch]]))

(def schema-to-load
  (select-keys ethlance-db/job-schema
               [:job/title :job/total-paid :job/created-on :job/hiring-done-on]))

(defn employer-jobs-open []
  (let [xs-width? (subscribe [:window/xs-width?])]
    (fn [{:keys [:user/id]}]
      [jobs-table
       {:list-subscribe [:list/jobs :list/employer-jobs-open]
        :initial-dispatch {:list-key :list/employer-jobs-open
                           :fn-key :ethlance-views/get-employer-jobs
                           :load-dispatch-key :contract.db/load-jobs
                           :schema schema-to-load
                           :args {:user/id id :job/status 1}}
        :all-ids-subscribe [:list/ids :list/employer-jobs-open]
        :show-created-on? true
        :show-total-paid? true
        :title [row
                {:start "sm"}
                [col {:xs 12 :sm 6}
                 [:h2 "Open Hiring Jobs"]]
                [col {:xs 12 :sm 6
                      :style (if @xs-width?
                               (styles/margin-vertical 10)
                               styles/text-right)}
                 [ui/raised-button
                  {:label "New Job"
                   :primary true
                   :icon (icons/plus)
                   :href (u/path-for :job/create)}]]]
        :no-items-text "You have no open hiring jobs"}])))

(defn employer-jobs-done []
  (let [xs-width? (subscribe [:window/xs-width?])]
    (fn [{:keys [:user/id]}]
      [jobs-table
       {:list-subscribe [:list/jobs :list/employer-jobs-done]
        :show-hiring-done-on? true
        :show-created-on? (not @xs-width?)
        :show-total-paid? true
        :initial-dispatch {:list-key :list/employer-jobs-done
                           :fn-key :ethlance-views/get-employer-jobs
                           :load-dispatch-key :contract.db/load-jobs
                           :schema schema-to-load
                           :args {:user/id id :job/status 2}}
        :all-ids-subscribe [:list/ids :list/employer-jobs-done]
        :title "Closed Hiring Jobs"
        :no-items-text "You have no closed hiring jobs"}])))


(defn employer-jobs-page []
  (let [user (subscribe [:db/active-user])]
    (fn []
      [misc/only-registered
       [misc/only-employer
        [center-layout
         [employer-jobs-open @user]
         [employer-jobs-done @user]]]])))