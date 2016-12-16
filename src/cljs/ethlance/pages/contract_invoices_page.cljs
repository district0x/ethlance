(ns ethlance.pages.contract-invoices-page
  (:require
    [cljs-react-material-ui.icons :as icons]
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.invoices-table :refer [invoices-table]]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a center-layout]]
    [ethlance.ethlance-db :as ethlance-db]
    [ethlance.styles :as styles]
    [re-frame.core :refer [subscribe dispatch]]
    [ethlance.utils :as u]))


(defn contract-invoices-page []
  (let [contract-id (subscribe [:contract/route-contract-id])
        contract (subscribe [:contract/detail])]
    (fn []
      (let [{:keys [:contract/total-paid :contract/total-invoiced]} @contract]
        [center-layout
         [invoices-table
          {:title "Contract Invoices"
           :list-subscribe [:list/invoices :list/contract-invoices]
           :initial-dispatch {:list-key :list/contract-invoices
                              :fn-key :ethlance-views/get-contract-invoices
                              :load-dispatch-key :contract.db/load-invoices
                              :schema ethlance-db/invoices-table-schema
                              :args {:contract/id @contract-id :invoice/status 0}}
           :show-freelancer? true
           :show-job? true
           :show-status? true
           :all-ids-subscribe [:list/ids :list/contract-invoices]}
          [row
           [col {:xs 12
                 :style styles/margin-top-gutter-less}
            [line "Total Invoiced" (u/eth total-invoiced)]]
           [col {:xs 12}
            [line "Total Paid" (u/eth total-paid)]]]
          [ui/raised-button
           {:primary true
            :href (u/path-for :contract/detail :contract/id @contract-id)
            :label "Go to Proposal"
            :icon (icons/navigation-chevron-left)}]]]))))