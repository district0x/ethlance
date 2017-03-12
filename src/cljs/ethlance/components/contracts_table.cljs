(ns ethlance.components.contracts-table
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.list-table :refer [list-table]]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a currency]]
    [ethlance.constants :as constants]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [medley.core :as medley]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    ))

(defn contracts-table-content []
  (let [xs-width? (subscribe [:window/xs-width?])]
    (fn [{:keys [:show-invitation? :show-proposal? :show-created-on? show-done-on?
                 :show-employer? :show-rate? :show-total-paid? :show-status? :show-freelancer?
                 :show-invitation-or-proposal-time? :show-job? :show-cancelled-on? :show-total-spent?
                 :no-items-text :initial-dispatch :all-ids-subscribe :highlight-row-pred]}
         {:keys [items offset limit loading?]}]
      (let [xs? @xs-width?]
        [ui/table
         [ui/table-header
          [ui/table-row
           (when show-job?
             [ui/table-header-column "Job"])
           (when show-employer?
             [ui/table-header-column "Employer"])
           (when show-freelancer?
             [ui/table-header-column "Freelancer"])
           (when show-rate?
             [ui/table-header-column "Rate"])
           (when show-invitation-or-proposal-time?
             [ui/table-header-column "Time"])
           (when show-total-paid?
             [ui/table-header-column "Total Earned"])
           (when show-total-spent?
             [ui/table-header-column "Total Spent"])
           (when show-invitation?
             [ui/table-header-column "Invited"])
           (when show-proposal?
             [ui/table-header-column "Proposed"])
           (when show-created-on?
             [ui/table-header-column "Proposal Accepted"])
           (when show-cancelled-on?
             [ui/table-header-column "Cancelled"])
           (when show-done-on?
             [ui/table-header-column "Contract Ended"])
           (when show-status?
             [ui/table-header-column "Status"])]]
         [ui/table-body
          {:show-row-hover true}
          (if (seq items)
            (for [item items]
              (let [{:keys [:contract/job :contract/rate :contract/id :proposal/rate :contract/total-paid
                            :contract/freelancer :contract/status]} item
                    {:keys [:job/title :job/employer :job/payment-type :job/reference-currency]} job]
                [ui/table-row
                 {:key id
                  :style (merge styles/clickable
                                (when (and highlight-row-pred (highlight-row-pred item))
                                  styles/table-highlighted-row))
                  :on-touch-tap (u/table-row-nav-to-fn :contract/detail {:contract/id id})}
                 (when show-job?
                   [ui/table-row-column
                    [a {:route-params {:job/id (:job/id job)}
                        :route :job/detail}
                     (:job/title job)]])
                 (when show-employer?
                   [ui/table-row-column
                    [a {:route-params {:user/id (:user/id employer)}
                        :route :employer/detail}
                     (:user/name employer)]])
                 (when show-freelancer?
                   [ui/table-row-column
                    [a
                     {:route-params {:user/id (:user/id freelancer)}
                      :route :freelancer/detail}
                     (:user/name freelancer)]])
                 (when show-rate?
                   [ui/table-row-column
                    (if (= status 1)
                      "-"
                      [misc/rate rate payment-type {:value-currency reference-currency}])])
                 (when show-invitation-or-proposal-time?
                   [ui/table-row-column
                    (if (= status 1)
                      (u/time-ago (:invitation/created-on item))
                      (u/time-ago (:proposal/created-on item)))])
                 (when (or show-total-paid? show-total-spent?)
                   [ui/table-row-column
                    [currency total-paid]])
                 (when show-invitation?
                   [ui/table-row-column
                    (u/time-ago (:invitation/created-on item))])
                 (when show-proposal?
                   [ui/table-row-column
                    (u/time-ago (:proposal/created-on item))])
                 (when show-created-on?
                   [ui/table-row-column
                    (u/time-ago (:contract/created-on item))])
                 (when show-cancelled-on?
                   [ui/table-row-column
                    (u/time-ago (:contract/cancelled-on item))])
                 (when show-done-on?
                   [ui/table-row-column
                    (u/time-ago (:contract/done-on item))])
                 (when show-status?
                   [ui/table-row-column
                    {:style (when xs? styles/table-row-column-thin)}
                    [misc/status-chip
                     {:background-color (styles/contract-status-colors status)
                      :style styles/table-status-chip
                      :on-touch-tap (u/nav-to-fn :contract/detail {:contract/id id})}
                     (constants/contract-statuses status)]])]))
            (misc/create-no-items-row (or no-items-text "No contracts sent yet") loading?))]
         (misc/create-table-pagination
           {:offset offset
            :limit limit
            :all-ids-subscribe all-ids-subscribe
            :list-db-path [(:list-key initial-dispatch)]
            :load-dispatch [(:load-dispatch-key initial-dispatch) (:fields initial-dispatch)]})]))))

(defn contracts-table [props]
  [list-table
   (r/merge-props
     props
     {:body contracts-table-content})])
