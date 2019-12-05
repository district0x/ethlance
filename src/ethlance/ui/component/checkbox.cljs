(ns ethlance.ui.component.checkbox
  (:require
   [reagent.core :as r]
   [ethlance.ui.component.inline-svg :refer [c-inline-svg]]))


(defn c-labeled-checkbox
  "Checkbox Input Component

  # Keyword Arguments

  opts - Optional Arguments

  # Optional Arguments (opts)

  :label - The label to display after the checkbox.

  :on-change - Accepts a function with one argument, which is called
  when the value of the checkbox changes between `true` and `false`.

  :default-checked? - Determines whether initially the checkbox is
  checked or not. [default: false]

  # Examples

  ```clojure
  [c-labeled-checkbox {:label \"I Agree to the Terms and Services.\"}]
  ```
  "
  [{:keys [label on-change default-checked?] :as opts}]
  (let [opts (dissoc opts :label :on-change :default-checked?)
        *checked? (r/atom default-checked?)]
    (fn [opts]
      [:div.ethlance-checkbox
       (merge
        opts
        {:on-click
         (fn []
           (when on-change
             (on-change @*checked?))
           (swap! *checked? not))

         :class (when @*checked? "checked")})
       [c-inline-svg {:src "/images/svg/checkbox.svg"
                      :width 24
                      :height 24}]
       [:span.label label]])))
       
