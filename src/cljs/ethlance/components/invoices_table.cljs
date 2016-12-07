(ns ethlance.components.invoices-table
  (:require
    [cljs-react-material-ui.icons :as icons]
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.list-pagination :refer [list-pagination]]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a]]
    [ethlance.constants :as constants]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [medley.core :as medley]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    ))

(defn invoices-table [{:keys [list-subscribe initial-dispatch]}]
  (let [list (subscribe list-subscribe)]
    (dispatch (into [:contract/initiate-load] initial-dispatch))
    (fn [{:keys [title show-freelancer? show-job? pagination-props]}]
      (let [{:keys [loading? items offset limit]} @list]
        [paper
         {:loading? loading?
          :style styles/paper-section-main}
         [:h2 (or "Invoices" title)]
         [ui/table
          [ui/table-header
           [ui/table-row
            (when show-freelancer?
              [ui/table-header-column "Freelancer"])
            (when show-job?
              [ui/table-header-column "Job"])
            [ui/table-header-column "Amount"]
            [ui/table-header-column "Created On"]
            [ui/table-header-column "Status"]]]
          [ui/table-body
           {:show-row-hover true}
           (if (seq items)
             (for [item items]
               (let [{:keys [:invoice/contract :invoice/id :invoice/created-on :invoice/status
                             :invoice/amount]} item
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
                     [a {:route-params {:job/id (:job/id freelancer)}
                         :route :freelancer/detail}
                      (:job/title job)]])
                  [ui/table-row-column
                   (u/eth amount)]
                  [ui/table-row-column
                   (u/time-ago created-on)]
                  [ui/table-row-column
                   [misc/status-chip
                    {:background-color (styles/invoice-status-colors status)}
                    (constants/invoice-status status)]]]))
             (misc/create-no-items-row "No invoices found" loading?))]
          (misc/create-table-pagination
            (r/merge-props
              {:offset offset
               :limit limit}
              pagination-props))]]))))
