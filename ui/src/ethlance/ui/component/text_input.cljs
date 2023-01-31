(ns ethlance.ui.component.text-input
  (:require
   [reagent.core :as r]))

(defn c-text-input
  "Default Text Input Component

  # Keyword Arguments

  opts - React Props
  "
  [{:keys [default-value color] :as opts}]
  (let [*dirty? (r/atom false)
        *current-value (r/atom default-value)
        class-color (case color
                      :primary "primary"
                      :secondary "secondary"
                      "primary")]
    (fn [{:keys [default-value value on-change error?]}]
      (assert (not (and value default-value))
              "Component has both controlled `value` and uncontrolled `default-value` attributes set.")
      (let [current-value (if (contains? opts :default-value) @*current-value value)
            opts (dissoc opts :default-value :value :color :on-change :error?)]
        [:input.ethlance-text-input
         (merge
           (dissoc opts :error)
           {:class [class-color (when error? "error") (when @*dirty? "dirty")]
            :value current-value
            :on-change (fn [e]
                         (reset! *dirty? true)
                         (let [target-value (-> e .-target .-value)]
                           (reset! *current-value target-value)
                           (when on-change (on-change target-value))))})]))))
