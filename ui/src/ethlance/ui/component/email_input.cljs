(ns ethlance.ui.component.email-input
  (:require
   [reagent.core :as r]))

(defn c-email-input
  [{:keys [default-value color]}]
  (let [*dirty? (r/atom false)
        *current-value (r/atom default-value)
        class-color (case color
                      :primary "primary"
                      :secondary "secondary"
                      "primary")]
    (fn [{:keys [default-value value on-change error?] :as opts}]
      (assert (not (and value default-value))
              "Component has both controlled `value` and uncontrolled `default-value` attributes set.")
      (let [current-value (if (contains? opts :default-value) @*current-value value)
            opts (dissoc opts :default-value :value :color :on-change :error?)]
        [:input.ethlance-email-input
         (merge
          opts
          {:class [class-color (when error? "error") (when @*dirty? "dirty")]
           :value current-value
           :on-change (fn [e]
                        (reset! *dirty? true)
                        (let [target-value (-> e .-target .-value)]
                          (reset! *current-value target-value)
                          (when on-change (on-change target-value))))})]))))
