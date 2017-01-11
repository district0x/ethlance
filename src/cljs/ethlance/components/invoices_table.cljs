(ns ethlance.components.invoices-table
  (:require
    [cljs-react-material-ui.icons :as icons]
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.list-table :refer [list-table]]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a]]
    [ethlance.constants :as constants]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [medley.core :as medley]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    ))

(defn invoices-table-content []
  (let [xs-width (subscribe [:window/xs-width?])]
    (fn [{:keys [:show-freelancer? :show-job? :show-status? :show-paid-on? :show-contract?
                 :always-show-created-on? :show-created-on? :no-items-text :initial-dispatch :all-ids-subscribe]}
         {:keys [items offset limit loading?]}]
      [ui/table
       [ui/table-header
        [ui/table-row
         (when show-freelancer?
           [ui/table-header-column "Freelancer"])
         (when show-job?
           [ui/table-header-column "Job"])
         [ui/table-header-column "Amount"]
         (when (or (not @xs-width) always-show-created-on?)
           [ui/table-header-column "Created On"])
         (when show-paid-on?
           [ui/table-header-column "Paid On"])
         (when show-contract?
           [ui/table-header-column "Contract"])
         (when show-status?
           [ui/table-header-column "Status"])]]
       [ui/table-body
        {:show-row-hover true}
        (if (seq items)
          (for [item items]
            (let [{:keys [:invoice/contract :invoice/id :invoice/created-on :invoice/status
                          :invoice/amount :invoice/paid-on]} item
                  {:keys [:contract/freelancer :contract/job]} contract]
              [ui/table-row
               {:key id
                :style styles/clickable
                :on-touch-tap (u/table-row-nav-to-fn :invoice/detail {:invoice/id id})}
               (when show-freelancer?
                 [ui/table-row-column
                  [a {:route-params {:user/id (:user/id freelancer)}
                      :route :freelancer/detail}
                   (:user/name freelancer)]])
               (when show-job?
                 [ui/table-row-column
                  [a {:route-params {:job/id (:job/id job)}
                      :route :job/detail}
                   (:job/title job)]])
               [ui/table-row-column
                (u/eth amount)]
               (when (or (not @xs-width) always-show-created-on?)
                 [ui/table-row-column
                  (u/time-ago created-on)])
               (when show-paid-on?
                 [ui/table-row-column
                  (u/time-ago paid-on)])
               (when show-contract?
                 [ui/table-row-column
                  [a {:route :contract/detail
                      :route-params (select-keys contract [:contract/id])}
                   "Go to Contract"]])
               (when show-status?
                 [ui/table-row-column
                  {:style (when @xs-width styles/table-row-column-thin)}
                  [misc/status-chip
                   {:background-color (styles/invoice-status-colors status)
                    :style styles/table-status-chip}
                   (constants/invoice-status status)]])]))
          (misc/create-no-items-row (or no-items-text "No invoices") loading?))]
       (misc/create-table-pagination
         {:offset offset
          :limit limit
          :all-ids-subscribe all-ids-subscribe
          :list-db-path [(:list-key initial-dispatch)]
          :load-dispatch [(:load-dispatch-key initial-dispatch) (:schema initial-dispatch)]})])))

(defn invoices-table [props]
  [list-table
   (r/merge-props
     {:body invoices-table-content
      :title "Invoices"}
     props)])
