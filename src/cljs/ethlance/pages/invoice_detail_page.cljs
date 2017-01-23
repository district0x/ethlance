(ns ethlance.pages.invoice-detail-page
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.message-bubble :refer [message-bubble]]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a currency]]
    [ethlance.constants :as constants]
    [ethlance.ethlance-db :as ethlance-db]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [goog.string :as gstring]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    ))

(defn invoice-description [{:keys [:invoice/description :invoice/contract]}]
  (when (seq description)
    [message-bubble
     {:side :left
      :user (:contract/freelancer contract)}
     description]))

(defn invoice-info [{:keys [:invoice/status :invoice/contract :invoice/created-on :invoice/amount
                            :invoice/worked-hours :invoice/worked-from :invoice/worked-to
                            :invoice/paid-on :invoice/cancelled-on]
                     :as invoice}]
  (let [{:keys [:contract/job :contract/freelancer]} contract]
    [row
     [invoice-description invoice]
     [col
      {:xs 12}
      [line "Created by" [a {:route-params (select-keys freelancer [:user/id])
                             :route :freelancer/detail}
                          (:user/name freelancer)]]
      [line "Job" [a {:route-params (select-keys job [:job/id])
                      :route :job/detail}
                   (:job/title job)]]
      [line "Proposal" [a {:route-params (select-keys contract [:contract/id])
                           :route :contract/detail}
                        (:contract/id contract)]]
      [line "Amount" [currency amount {:full-length? true}]]
      [line "Created on" (u/format-datetime created-on)]
      [line "Worked hours" worked-hours]
      [line "Worked from" (u/format-date worked-from)]
      [line "Worked to" (u/format-date worked-to)]
      (when paid-on
        [line "Paid on" (u/format-datetime paid-on)])
      (when cancelled-on
        [line "Cancelled on" (u/format-datetime cancelled-on)])]]))

(defn invoice-detail-page []
  (let [invoice (subscribe [:invoice/detail])
        invoice-id (subscribe [:invoice/route-invoice-id])
        by-me? (subscribe [:invoice/by-me?])
        for-me (subscribe [:invoice/for-me?])
        form-pay (subscribe [:form.invoice/pay-invoice])
        form-cancel (subscribe [:form.invoice/cancel-invoice])]
    (dispatch [:after-eth-contracts-loaded [:contract.db/load-invoices ethlance-db/invoice-schema [@invoice-id]]])
    (fn []
      (let [{:keys [:invoice/contract :invoice/id :invoice/created-on :invoice/status
                    :invoice/amount]} @invoice
            job-title (get-in contract [:contract/job :job/title])]
        [misc/center-layout
         [paper
          {:loading? (or (empty? job-title)
                         (:loading? @form-pay)
                         (:loading? @form-cancel))
           :style styles/paper-section-main}
          [row-plain
           {:middle "xs"
            :between "xs"
            :style styles/margin-bottom-gutter}
           [:h1 "Invoice #" id]
           [misc/status-chip
            {:background-color (styles/invoice-status-colors status)}
            (constants/invoice-status status)]]
          (when (seq job-title)
            [:div
             [invoice-info @invoice]
             [row-plain
              {:end "xs"}
              (when (and (= status 1) @by-me?)
                [ui/raised-button
                 {:secondary true
                  :label "Cancel Invoice"
                  :disabled (:loading? @form-cancel)
                  :style styles/margin-top-gutter-less
                  :on-touch-tap #(dispatch [:contract.invoice/cancel-invoice {:invoice/id id}])}])
              (when (and (= status 1) @for-me)
                [ui/raised-button
                 {:primary true
                  :label "Pay Invoice"
                  :disabled (:loading? @form-pay)
                  :style styles/margin-top-gutter-less
                  :on-touch-tap #(dispatch [:contract.invoice/pay-invoice {:invoice/id id} amount])}])]])]]))))