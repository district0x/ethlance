(ns ethlance.pages.employer-invoices-page
  (:require
    [cljs-react-material-ui.icons :as icons]
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.invoices-table :refer [invoices-table]]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a center-layout]]
    [ethlance.ethlance-db :as ethlance-db]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [re-frame.core :refer [subscribe dispatch]]
    ))

(defn invoices-stats [{:keys [:employer/total-paid :employer/total-invoiced]}]
  [paper
   [row
    [col {:xs 12}
     [:h2 {:style styles/margin-bottom-gutter-less} "My Invoices as Employer"]
     [:div
      [:h3 "Total to Pay: " (u/eth total-invoiced)]
      [:h3 "Total Paid: " (u/eth total-paid)]]]]])

(defn employer-pending-invoices [{:keys [:user/id]}]
  [invoices-table
   {:list-subscribe [:list/invoices :list/employer-invoices-pending]
    :show-freelancer? true
    :show-job? true
    :initial-dispatch {:list-key :list/employer-invoices-pending
                       :fn-key :ethlance-views/get-employer-invoices
                       :load-dispatch-key :contract.db/load-invoices
                       :schema ethlance-db/invoices-table-schema
                       :args {:user/id id :invoice/status 1}}
    :all-ids-subscribe [:list/ids :list/employer-invoices-pending]
    :title "Pending Invoices"
    :no-items-text "You have no invoices to pay"}])

(defn employer-paid-invoices [{:keys [:user/id]}]
  [invoices-table
   {:list-subscribe [:list/invoices :list/employer-invoices-paid]
    :show-freelancer? true
    :show-job? true
    :show-paid-on? true
    :initial-dispatch {:list-key :list/employer-invoices-paid
                       :fn-key :ethlance-views/get-employer-invoices
                       :load-dispatch-key :contract.db/load-invoices
                       :schema ethlance-db/invoices-table-schema
                       :args {:user/id id :invoice/status 2}}
    :all-ids-subscribe [:list/ids :list/employer-invoices-paid]
    :title "Paid Invoices"
    :no-items-text "You have no paid invoices"}])

(defn employer-invoices-page []
  (let [user (subscribe [:db/active-user])]
    (fn []
      [misc/only-registered
       [misc/only-employer
        [center-layout
         [invoices-stats @user]
         [employer-pending-invoices @user]
         [employer-paid-invoices @user]]]])))
