(ns ethlance.ui.component.text-input
  (:require
   [reagent.core :as r]))


(defn c-text-input
  "Default Text Input Component

  # Keyword Arguments
  
  opts - React Props
  "
  [{:keys [default-value value color on-change] :as opts}]
  (let [*current-value (r/atom default-value)
        class-color (case color
                      :primary "primary"
                      :secondary "secondary"
                      "primary")]
    (fn [{:keys [default-value value color on-change] :as opts}]
      (assert (not (and value default-value))
              "Component has both controlled `value` and uncontrolled `default-value` attributes set.")
      (let [opts (dissoc opts :default-value :value :color :on-change)
            current-value (if (contains? opts :default-value) @*current-value value)]
        [:input.ethlance-text-input
         (merge
          opts
          {:class [class-color]
           :value current-value
           :on-change (fn [e] 
                        (reset! *current-value (-> e .-target .-value))
                        (when on-change (on-change (-> e .-target .-value))))})]))))
