(ns ethlance.pages.contract-invoices-page
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.icons :as icons]
    [ethlance.components.invoices-table :refer [invoices-table]]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a center-layout currency]]
    [ethlance.ethlance-db :as ethlance-db]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [re-frame.core :refer [subscribe dispatch]]))


(defn contract-invoices-header [{:keys [:contract/job :contract/freelancer]}]
  (when (and (:job/id job) (:user/id freelancer))
    [:div {:style styles/margin-top-gutter-less}
     [:div
      [a {:route :job/detail
          :route-params (select-keys job [:job/id])}
       (:job/title job)]]
     [:div
      [a {:route :freelancer/detail
          :route-params (select-keys freelancer [:user/id])}
       (:user/name freelancer)]]]))

(defn contract-invoices-page []
  (let [contract-id (subscribe [:contract/route-contract-id])
        contract (subscribe [:contract/detail])]
    (fn []
      (let [{:keys [:contract/total-paid :contract/total-invoiced]} @contract]
        [center-layout
         [invoices-table
          {:title "Contract Invoices"
           :header (partial contract-invoices-header @contract)
           :list-subscribe [:list/invoices :list/contract-invoices]
           :initial-dispatch {:list-key :list/contract-invoices
                              :fn-key :ethlance-views/get-contract-invoices
                              :load-dispatch-key :contract.db/load-invoices
                              :schema ethlance-db/invoices-table-schema
                              :args {:contract/id @contract-id :invoice/status 0}}
           :show-status? true
           :always-show-created-on? true
           :all-ids-subscribe [:list/ids :list/contract-invoices]}
          [row
           [col {:xs 12
                 :style styles/margin-top-gutter-less}
            [line "Total Invoiced" [currency total-invoiced]]]
           [col {:xs 12}
            [line "Total Paid" [currency total-paid]]]]
          [ui/raised-button
           {:primary true
            :href (u/path-for :contract/detail :contract/id @contract-id)
            :label "Go to Proposal"
            :icon (icons/chevron-left)}]]]))))