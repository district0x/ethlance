(ns ethlance.ui.component.circle-button
  "Circle button, which usually displays an icon"
  (:require
   [ethlance.ui.component.icon :refer [c-icon]]))


(defn c-circle-icon-button
  "Circular Button Component with an icon.

  # Keyword Arguments

  opts - Optional Arguments

  # Optional Arguments

  :name - Keyword name of the icon to use. List of allowed keywords
  can be found in the ethlance.ui.component.icon namespace.

  :color - Color scheme to use with the component. `:primary`,
  `:secondary`, or `:none`. [default: :primary].

  :size - Size of the circular button. `:small`, `:normal`,
  `:large`. [default: :normal].

  :disabled? - If true, sets the 'disabled' class.

  :hide? - If true, sets the 'hide' class, which will precent
  displaying of the component.

  :on-click - Provide an on-click handler, for when the button is pressed.

  # Examples

  ```clojure
  [c-circle-button {:name :github :on-click #(log/info \"You want to navigate to the github page\")}]
  ```
  "
  []
  (fn [{:keys [name color size disabled? hide? on-click]
        :or {name :about color :primary size :normal}
        :as opts}]
    (let [opts (dissoc opts :name :color :size :disabled? :hide? :on-click)
          class-color (case color
                        :primary "primary"
                        :secondary "secondary"
                        :none "")
          class-size  (case size
                        :smaller "smaller"
                        :small "small"
                        :normal ""
                        :large "large")
          class-disabled (when disabled? "disabled")
          class-hide (when hide? "hide")]
      [:a.ethlance-circle-button.ethlance-circle-icon-button
       (merge
        opts
        {:class [class-color class-size class-disabled class-hide]
         :on-click
         (fn [e]
           (when (and on-click (not disabled?))
             (on-click e)))})
       [c-icon {:name name :color color :size size}]])))
