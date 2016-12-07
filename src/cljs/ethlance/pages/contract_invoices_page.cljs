(ns ethlance.pages.contract-invoices-page
  (:require
    [ethlance.components.invoices-table :refer [invoices-table]]
    [ethlance.ethlance-db :as ethlance-db]
    [re-frame.core :refer [subscribe dispatch]]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a center-layout]]
    ))


(defn contract-invoices-page []
  (let [contract-id (subscribe [:contract/route-contract-id])]
    (fn []
      [center-layout
       [invoices-table
        {:title "Contract Invoices"
         :list-subscribe [:list/contract-invoices]
         :initial-dispatch [:contract.views/load-contract-invoices {:contract/id @contract-id :invoice/status 0}]
         :show-freelancer? true
         :show-job? true
         :pagination-props {:all-subscribe [:list.ids/contract-invoices]
                            :list-db-path [:list/contract-invoices]
                            :load-dispatch [:contract.db/load-invoices ethlance-db/invoices-table-schema]}}]])))