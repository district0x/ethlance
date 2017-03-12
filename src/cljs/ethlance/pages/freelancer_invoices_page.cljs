(ns ethlance.pages.freelancer-invoices-page
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.icons :as icons]
    [ethlance.components.invoices-table :refer [invoices-table]]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a center-layout currency]]
    [ethlance.ethlance-db :as ethlance-db]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [re-frame.core :refer [subscribe dispatch]]))

(defn invoices-stats [{:keys [:freelancer/total-earned :freelancer/total-invoiced]}]
  [paper
   [row
    [col {:xs 12}
     [:h2 {:style styles/margin-bottom-gutter-less} "My Invoices as Freelancer"]
     [row {:bottom "xs"}
      [col {:xs 12 :md 6}
       [:div
        [:h3 "Total Invoiced: " [currency total-invoiced]]
        [:h3 "Total Earned: " [currency total-earned]]]]
      [col
       {:xs 12 :md 6
        :style styles/text-right}
       [ui/raised-button
        {:primary true
         :label "New Invoice"
         :icon (icons/plus)
         :style styles/margin-top-gutter-less
         :href (u/path-for :invoice/create)}]]]]]])

(defn freelancer-pending-invoices [{:keys [:user/id]}]
  [invoices-table
   {:list-subscribe [:list/invoices :list/freelancer-invoices-pending]
    :show-job? true
    :show-contract? true
    :initial-dispatch {:list-key :list/freelancer-invoices-pending
                       :fn-key :ethlance-views/get-freelancer-invoices
                       :load-dispatch-key :contract.db/load-invoices
                       :fields ethlance-db/invoices-table-entity-fields
                       :args {:user/id id :invoice/status 1}}
    :all-ids-subscribe [:list/ids :list/freelancer-invoices-pending]
    :title "Pending Invoices"
    :no-items-text "You have no pending invoices"}])

(defn freelancer-paid-invoices [{:keys [:user/id]}]
  (let [xs-width? (subscribe [:window/xs-width?])]
    [invoices-table
     {:list-subscribe [:list/invoices :list/freelancer-invoices-paid]
      :show-job? true
      :show-paid-on? (not @xs-width?)
      :show-contract? true
      :initial-dispatch {:list-key :list/freelancer-invoices-paid
                         :fn-key :ethlance-views/get-freelancer-invoices
                         :load-dispatch-key :contract.db/load-invoices
                         :fields ethlance-db/invoices-table-entity-fields
                         :args {:user/id id :invoice/status 2}}
      :all-ids-subscribe [:list/ids :list/freelancer-invoices-paid]
      :title "Paid Invoices"
      :no-items-text "You have no paid invoices"}]))

(defn freelancer-invoices-page []
  (let [user (subscribe [:db/active-user])]
    (fn []
      [misc/only-registered
       [misc/only-freelancer
        [center-layout
         [invoices-stats @user]
         [freelancer-pending-invoices @user]
         [freelancer-paid-invoices @user]]]])))
