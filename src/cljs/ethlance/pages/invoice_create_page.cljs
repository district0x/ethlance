(ns ethlance.pages.invoice-create-page
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a center-layout]]
    [ethlance.constants :as constants]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [medley.core :as medley]
    [goog.string :as gstring]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [ethlance.ethlance-db :as ethlance-db]
    [cljs-time.coerce :as coerce]))

(defn dispatch-contracts-load [user-id]
  (when-not goog.DEBUG
    (dispatch [:form/set-value :form.invoice/add-invoice :invoice/contract 0 false]))
  (dispatch [:after-eth-contracts-loaded
             [:list/load-ids {:list-key :list/freelancer-my-open-contracts
                              :fn-key :ethlance-views/get-freelancer-contracts
                              :load-dispatch-key :contract.db/load-contracts
                              :fields #{:contract/job
                                        :proposal/rate
                                        :job/title
                                        :job/payment-type
                                        :job/reference-currency}
                              :args {:user/id user-id :contract/statuses [3] :job/statuses [1 2]}}]]))

(defn worked-time-field [{:keys [:contract/id :field-key :validator] :as props}]
  [ui/text-field
   (r/merge-props
     {:floating-label-fixed true
      :type :number
      :min 0
      :disabled (zero? id)
      :style styles/display-block
      :on-change (fn [_ value]
                   (let [value (js/parseInt value)]
                     (when-not (js/isNaN value)
                       (dispatch [:form.invoice/add-invoice-localstorage
                                  id
                                  field-key
                                  value
                                  validator]))))}
     (dissoc props :contract/id :field-key :validator))])

(defn add-invoice-form []
  (let [contracts-list (subscribe [:list/contracts :list/freelancer-my-open-contracts])
        form (subscribe [:form.invoice/add-invoice-prefilled])]
    (fn []
      (let [{:keys [:data :loading? :errors :needs-conversion? :contract :hourly-rate? :total-amount]} @form
            {:keys [:invoice/description :invoice/amount :invoice/worked-hours :invoice/worked-from
                    :invoice/worked-to :invoice/conversion-rate :invoice/rate :invoice/worked-minutes]} data
            {:keys [:job/reference-currency :job/payment-type]} (:contract/job contract)
            contract-id (:invoice/contract data)
            contracts (:items @contracts-list)]
        [paper
         {:loading? (or loading? (:loading? @contracts-list))}
         [:h2 "New Invoice"]
         [:div
          [ui/select-field
           {:floating-label-text "Job"
            :value (when (pos? contract-id) contract-id)
            :auto-width true
            :style styles/overflow-ellipsis
            :disabled (empty? contracts)
            :on-change #(dispatch [:form/set-value :form.invoice/add-invoice :invoice/contract %3])}
           (for [{:keys [:contract/id :contract/job]} contracts]
             [ui/menu-item
              {:value id
               :primary-text (gstring/format "%s (#%s)" (:job/title job) (:job/id job))
               :key id}])]]
         [worked-time-field
          {:floating-label-text "Worked hours"
           :contract/id contract-id
           :field-key :invoice/worked-hours
           :validator u/non-neg?
           :value worked-hours}]
         [worked-time-field
          {:floating-label-text "Worked minutes"
           :contract/id contract-id
           :field-key :invoice/worked-minutes
           :validator (partial > 60)
           :value worked-minutes
           :max 59
           :error-text (when (< 59 worked-minutes)
                         "Choose value between 0 and 59")}]
         (when (pos? contract-id)
           [misc/ether-field-with-currency
            {:floating-label-text (cond
                                    hourly-rate? (constants/payment-types 1)
                                    (and (not hourly-rate?) needs-conversion?) (constants/payment-types 2)
                                    (and (not hourly-rate?) (not needs-conversion?)) "Invoiced amount")
             :value rate
             :floating-label-fixed true
             :form-key :form.invoice/add-invoice
             :field-key :invoice/rate
             :currency reference-currency
             :on-change (fn [_ value]
                          (dispatch [:form.invoice/add-invoice-localstorage
                                     (:contract/id contract)
                                     :invoice/rate
                                     value
                                     u/non-neg-ether-value?]))}])
         (when (and needs-conversion? hourly-rate?)
           [misc/ether-field-with-currency
            {:floating-label-text "Total"
             :value total-amount
             :disabled true
             :input-style {:color styles/text-color}
             :currency-style {:color styles/text-color}
             :currency reference-currency}])
         (when needs-conversion?
           [:h3
            {:style styles/margin-top-gutter-less}
            [misc/conversion-rate {:value conversion-rate :currency reference-currency}]])
         (when (or needs-conversion? hourly-rate?)
           [misc/ether-field-with-currency
            {:floating-label-text "Invoiced Amount"
             :value amount
             :disabled true
             :input-style {:color styles/text-color}
             :currency-style {:color styles/text-color}
             :currency 0}])
         [:div
          [ui/date-picker
           {:default-date (js/Date. worked-from)
            :max-date (js/Date.)
            :floating-label-text "Worked From"
            :disabled (zero? contract-id)
            :on-change #(dispatch [:form/set-value :form.invoice/add-invoice :invoice/worked-from
                                   (coerce/to-date-time %2)])}]]
         [:div
          [ui/date-picker
           {:default-date (js/Date. worked-to)
            :max-date (js/Date.)
            :floating-label-text "Worked To"
            :disabled (zero? contract-id)
            :on-change #(dispatch [:form/set-value :form.invoice/add-invoice :invoice/worked-to
                                   (coerce/to-date-time %2)])}]]
         [misc/textarea
          {:floating-label-text "Message"
           :form-key :form.invoice/add-invoice
           :field-key :invoice/description
           :max-length-key :max-invoice-description
           :value description
           :disabled (zero? contract-id)
           :hint-text misc/privacy-warning-hint}]
         [misc/send-button
          {:disabled (or loading? (boolean (seq errors)))
           :on-touch-tap #(dispatch [:contract.invoice/add-invoice
                                     (-> data
                                       (update :invoice/worked-from u/date->sol-timestamp)
                                       (update :invoice/worked-to u/date->sol-timestamp))])}]]))))

(defn invoice-create-page []
  (fn []
    [misc/only-registered
     [misc/only-freelancer
      {:on-user-change dispatch-contracts-load}
      [center-layout
       [add-invoice-form]]]]))
