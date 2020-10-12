(ns ethlance.ui.component.textarea-input
  (:require
   [reagent.core :as r]))

(defn c-textarea-input
  "Default TextArea Input Component

  # Keyword Arguments

  opts - React Props
  "
  [{:keys [default-value color]}]
  (let [*current-value (r/atom default-value)
        class-color (case color
                      :primary "primary"
                      :secondary "secondary"
                      "primary")]
    (fn [{:keys [default-value value on-change] :as opts}]
      (assert (not (and value default-value))
              "Component has both controlled `value` and uncontrolled `default-value` attributes set.")
      (let [current-value (if (contains? opts :default-value) @*current-value value)
            opts (dissoc opts :default-value :value :color :on-change)]
        [:textarea.ethlance-textarea-input
         (merge
          opts
          {:class [class-color]
           :value current-value
           :on-change (fn [e]
                        (let [target-value (-> e .-target .-value)]
                          (reset! *current-value target-value)
                          (when on-change (on-change target-value))))})]))))
