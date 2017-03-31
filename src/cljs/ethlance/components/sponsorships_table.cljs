(ns ethlance.components.sponsorships-table
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.icons :as icons]
    [ethlance.components.list-table :refer [list-table]]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a currency]]
    [ethlance.constants :as constants]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [medley.core :as medley]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn sponsorships-table-content []
  (fn [{:keys [:show-job? :show-name? :show-refunded-amount? :show-updated-on? :no-items-text :initial-dispatch :all-ids-subscribe
               :show-position-number? :link-to-sponsor-detail?]}
       {:keys [items offset limit loading?]}]
    [ui/table
     [ui/table-header
      [ui/table-row
       (when show-position-number?
         [ui/table-header-column
          {:style styles/table-position-column}
          ""])
       (when show-job?
         [ui/table-header-column "Job"])
       (when show-name?
         [ui/table-header-column "Name"])
       [ui/table-header-column "Amount"]
       (when show-refunded-amount?
         [ui/table-header-column "Refunded"])
       (when show-updated-on?
         [ui/table-header-column "Last Sponsorship"])]]
     [ui/table-body
      {:show-row-hover link-to-sponsor-detail?}
      (if (seq items)
        (for [[i item] (medley/indexed items)]
          (let [{:keys [:sponsorship/id :sponsorship/amount :sponsorship/name :sponsorship/link
                        :sponsorship/updated-on :sponsorship/refunded? :sponsorship/refunded-amount
                        :sponsorship/job :sponsorship/user]} item
                name (if (empty? name) [:i "Anonymous"] name)
                position (inc (+ offset i))]
            [ui/table-row
             (merge
               {:key id}
               (when (and link-to-sponsor-detail? user)
                 {:style styles/clickable
                  :on-touch-tap (u/table-row-nav-to-fn :sponsor/detail {:user/id user})})) ;; TODO change after user-id migration
             (when show-position-number?
               [ui/table-row-column
                {:style (merge styles/table-position-column
                               styles/text-center)}
                (if (contains? #{1 2 3} position)
                  [:img {:src (icons/medals-src position)
                         :alt (str position ". ")
                         :style {:width 22
                                 :margin-top 7}}]
                  (str position ". "))])
             (when show-job?
               [ui/table-row-column
                (when job
                  [a {:route-params (select-keys job [:job/id])
                      :route :job/detail}
                   (:job/title job)])])
             (when show-name?
               [ui/table-row-column
                (if (u/http-url? link)
                  [:a {:href link
                       :target :_blank
                       :style {:color styles/primary1-color}}
                   name]
                  name)])
             [ui/table-row-column
              [currency amount]]
             (when show-refunded-amount?
               [ui/table-row-column
                (if refunded?
                  [currency refunded-amount]
                  "-")])
             (when show-updated-on?
               [ui/table-row-column
                (u/time-ago updated-on)])]))
        (misc/create-no-items-row (or no-items-text "No sponsorships") loading?))]
     (misc/create-table-pagination
       {:offset offset
        :limit limit
        :all-ids-subscribe all-ids-subscribe
        :list-db-path [(:list-key initial-dispatch)]
        :load-dispatch [(:load-dispatch-key initial-dispatch) (:fields initial-dispatch)]})]))

(defn sponsorships-table [props & children]
  (into
    [list-table
     (r/merge-props
       {:body sponsorships-table-content
        :title "Sponsorships"}
       props)]
    children))
