(ns ethlance.components.jobs-table
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

(defn jobs-table-content [{:keys [:show-hiring-done-on? :no-items-text :initial-dispatch :all-ids-subscribe
                                  :show-status? :show-created-on? :show-total-paid?]}
                          {:keys [items offset limit loading?]}]
  [ui/table
   [ui/table-header
    [ui/table-row
     [ui/table-header-column "Title"]
     (when show-total-paid?
       [ui/table-header-column "Total Spent"])
     (when show-created-on?
       [ui/table-header-column "Created"])
     (when show-hiring-done-on?
       [ui/table-header-column "Hiring Closed"])
     (when show-status?
       [ui/table-header-column "Status"])]]
   [ui/table-body
    {:show-row-hover true}
    (if (seq items)
      (for [item items]
        (let [{:keys [:job/title :job/id :job/created-on :job/hiring-done-on :job/total-paid
                      :job/status]} item]
          [ui/table-row
           {:key id
            :style styles/clickable
            :on-touch-tap (u/table-row-nav-to-fn :job/detail {:job/id id})}
           [ui/table-row-column
            title]
           (when show-total-paid?
             [ui/table-row-column
              [currency total-paid]])
           (when show-created-on?
             [ui/table-row-column
              (u/time-ago created-on)])
           (when show-hiring-done-on?
             [ui/table-row-column
              (u/time-ago hiring-done-on)])
           (when show-status?
             [ui/table-row-column
              [misc/status-chip
               {:background-color (styles/job-status-colors status)
                :style styles/table-status-chip}
               (constants/job-statuses status)]])]))
      (misc/create-no-items-row no-items-text loading?))]
   (misc/create-table-pagination
     {:offset offset
      :limit limit
      :all-ids-subscribe all-ids-subscribe
      :list-db-path [(:list-key initial-dispatch)]
      :load-dispatch [(:load-dispatch-key initial-dispatch) (:schema initial-dispatch)]})])

(defn jobs-table [props]
  [list-table
   (r/merge-props
     {:body jobs-table-content}
     props)])
