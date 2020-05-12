(ns ethlance.ui.component.email-input
  (:require
   [reagent.core :as r]))


(defn c-email-input
  "Email Input Component

   # Notes

   - TODO: determine whether a valid email was entered."
  [{:keys [default-value value color on-change] :as opts}]
  (let [*current-value (r/atom default-value)
        class-color (case color
                      :primary "primary"
                      :secondary "secondary"
                      "primary")]
    (fn [{:keys [default-value value color on-change] :as opts}]
      (assert (not (and value default-value))
              "Component has both controlled `value` and uncontrolled `default-value` attributes set.")
      (let [current-value (if (contains? opts :default-value) @*current-value value)
            opts (dissoc opts :default-value :value :color :on-change)]
        [:input.ethlance-email-input
         (merge
          opts
          {:class [class-color]
           :value current-value
           :on-change (fn [e]
                        (let [target-value (-> e .-target .-value)]
                          (reset! *current-value target-value)
                          (when on-change (on-change target-value))))})]))))
