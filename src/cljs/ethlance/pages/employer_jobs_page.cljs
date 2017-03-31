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

(defn employer-jobs []
  (let [xs-width? (subscribe [:window/xs-width?])]
    (fn [{:keys [:user/id]}]
      [jobs-table
       {:list-subscribe [:list/jobs :list/employer-jobs]
        :show-status? true
        :show-created-on? (not @xs-width?)
        :show-total-paid? (not @xs-width?)
        :initial-dispatch {:list-key :list/employer-jobs
                           :fn-key :ethlance-views/get-employer-jobs
                           :load-dispatch-key :contract.db/load-jobs
                           :fields #{:job/title :job/total-paid :job/created-on :job/status}
                           :args {:user/id id :job/status 0}}
        :all-ids-subscribe [:list/ids :list/employer-jobs]
        :title [row
                {:start "sm"}
                [col {:xs 12 :sm 6}
                 [:h2 "My Jobs"]]
                [col {:xs 12 :sm 6
                      :style (if @xs-width?
                               (styles/margin-vertical 10)
                               styles/text-right)}
                 [ui/raised-button
                  {:label "New Job"
                   :primary true
                   :icon (icons/plus)
                   :href (u/path-for :job/create)}]]]
        :no-items-text "This employer didn't create any jobs"}])))


(defn employer-jobs-page []
  (let [user (subscribe [:db/active-user])]
    (fn []
      [misc/only-registered
       [misc/only-employer
        [center-layout
         [employer-jobs @user]]]])))