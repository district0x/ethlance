(ns ethlance.pages.invoice-create-page
  (:require
    [cljs-react-material-ui.icons :as icons]
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a center-layout]]
    [ethlance.constants :as constants]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [medley.core :as medley]
    [goog.string :as gstring]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [ethlance.ethlance-db :as ethlance-db]))

(defn dispatch-contracts-load [user-id]
  (dispatch [:form/value-changed :form.invoice/add-invoice :invoice/contract 0 false])
  (dispatch [:after-eth-contracts-loaded
             [:list/load-ids {:list-key :list/freelancer-my-open-contracts
                              :fn-key :views/get-freelancer-contracts
                              :load-dispatch-key :contract.db/load-contracts
                              :schema (select-keys ethlance-db/contract-schema [:contract/job #_:contract/freelancer])
                              :args {:user/id user-id :contract/status 3 :job/status 0}}]]))

(defn add-invoice-form []
  (let [contracts-list (subscribe [:list/freelancer-my-open-contracts])
        form (subscribe [:form.invoice/add-invoice])]
    (fn []
      (let [{:keys [:data :loading? :errors]} @form
            {:keys [:invoice/contract :invoice/description :invoice/amount :invoice/worked-hours :invoice/worked-from
                    :invoice/worked-to]} data
            contracts (:items @contracts-list)]
        [:div
         [:div
          [ui/select-field
           {:floating-label-text "Job"
            :value contract
            :auto-width true
            :style styles/overflow-ellipsis
            :on-change #(dispatch [:form/value-changed :form.invoice/add-invoice :invoice/contract %3])}
           (for [{:keys [:contract/id :contract/job]} contracts]
             [ui/menu-item
              {:value id
               :primary-text (gstring/format "%s (#%s)" (:job/title job) (:job/id job))
               :key id}])]]
         [:div
          [misc/ether-field
           {:floating-label-text "Amount in Ether"
            :default-value amount
            :form-key :form.invoice/add-invoice
            :field-key :invoice/amount}]]
         [:div
          [ui/text-field
           {:floating-label-text "Hours Worked"
            :default-value worked-hours
            :type :number
            :min 0
            :on-change #(dispatch [:form/value-changed :form.invoice/add-invoice :invoice/worked-hours (js/parseInt %2)])}]]
         [:div
          [ui/date-picker
           {:default-date (js/Date. (u/timestamp-sol->js worked-from))
            :max-date (js/Date.)
            :floating-label-text "Worked From"
            :on-change #(dispatch [:form/value-changed :form.invoice/add-invoice :invoice/worked-from
                                   (u/timestamp-js->sol (u/get-time %2))])}]]
         [:div
          [ui/date-picker
           {:default-date (js/Date. (u/timestamp-sol->js worked-to))
            :max-date (js/Date.)
            :floating-label-text "Worked To"
            :on-change #(dispatch [:form/value-changed :form.invoice/add-invoice :invoice/worked-to
                                   (u/timestamp-js->sol (u/get-time %2))])}]]
         [misc/textarea
          {:floating-label-text "Message"
           :form-key :form.invoice/add-invoice
           :field-key :invoice/description
           :max-length-key :max-invoice-description
           :default-value description}]
         [misc/send-button
          {:disabled (or loading? (boolean (seq errors)))
           :on-touch-tap #(dispatch [:contract.invoice/add data])}]]))))

(defn invoice-create-page []
  (fn []
    [misc/freelancer-only-page
     {:on-user-change dispatch-contracts-load}
     [center-layout
      [paper
       [:h2 "New Invoice"]
       [add-invoice-form]]]]))
