(ns ethlance.ui.page.new-invoice.subscriptions
  (:require
   [re-frame.core :as re]

   [ethlance.ui.util.tokens :as util.tokens]
   [district.ui.conversion-rates.queries :as rates-queries]
   [district.ui.conversion-rates.subs :as rates-subs]
   [ethlance.ui.page.new-invoice.events :as new-invoice.events]
   [ethlance.ui.subscription.utils :as subscription.utils]))


(def create-get-handler #(subscription.utils/create-get-handler new-invoice.events/state-key %))


;;
;; Registered Subscriptions
;;


(re/reg-sub :page.new-invoice/job-listing (create-get-handler :job-listing))
(re/reg-sub :page.new-invoice/invoiced-job (create-get-handler :invoiced-job))
(re/reg-sub :page.new-invoice/hours-worked (create-get-handler :hours-worked))
(re/reg-sub :page.new-invoice/hourly-rate (create-get-handler :hourly-rate))
(re/reg-sub :page.new-invoice/invoice-amount (create-get-handler :invoice-amount))
(re/reg-sub :page.new-invoice/message (create-get-handler :message))

(re/reg-sub
  :page.new-invoice/token-details
  (fn [db _]
    (let [token-details (get-in db [:district.ui.server-config :token-details])]
      (reduce (fn [acc detail] (assoc acc (:id detail) detail)) {} token-details))))

(re/reg-sub
  :page.new-invoice/job-token
  :<- [:page.new-invoice/invoiced-job]
  (fn [job _]
    {:type (-> job :job :job/token-type)
     :address (-> job :job :job/token-address)
     :amount (-> job :job :job/token-amount)
     :id (-> job :job :job/token-id)
     :name (-> job :job :token-details :token-detail/name)
     :symbol (keyword (-> job :job :token-details :token-detail/symbol))}))

(re/reg-sub
  :page.new-invoice/estimated-usd
  :<- [:page.new-invoice/job-token]
  :<- [:page.new-invoice/invoice-amount]
  :<- [::rates-subs/conversion-rates]
  (fn [[token-details invoice-amount conversion-rates] _]
    (let [from-currency (or (:symbol token-details) :ETH)
          to-currency :USD
          amount (or invoice-amount 0)
          rate (get-in conversion-rates [from-currency to-currency])
          sum-usd (util.tokens/round 2 (* amount rate))]
      (if from-currency
        (str "Estimated $" sum-usd " (1 " (name from-currency) " = " rate " " (name to-currency) ")")
        "Please fill in invoice details to get an estimate"))))
