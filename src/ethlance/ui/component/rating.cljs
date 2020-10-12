(ns ethlance.ui.component.rating
  (:require [reagent.core :as r]))

(def rating-star-src-primary "/images/icons/ethlance-star-icon-primary.svg")
(def rating-star-src-black "/images/icons/ethlance-star-icon-black.svg")
(def rating-star-src-white "/images/icons/ethlance-star-icon-white.svg")

(defn c-star []
  (fn [{:keys [active? color index on-change size]
        :or {color :primary size :default}}]
    (let [color-src (case color
                      :primary rating-star-src-primary
                      :white   rating-star-src-white
                      :black   rating-star-src-black)
          size-class (case size
                       :normal  ""
                       :default ""
                       :large   "large"
                       :small   "small")
          size-value (case size
                       :normal  24
                       :default 24
                       :large   36
                       :small   18)
          active-class (when active? "active")]
      [:img.star
       {:key (str "c-star-" index)
        :src color-src
        :on-click #(on-change index)
        :style {:width (str size-value "px")
                :height (str size-value "px")}
        :class [active-class size-class]}])))

(defn c-rating
  "Rating Component, for displaying feedback within ethlance.

  # Keyword Arguments

  opts - Optional Arguments

  # Optional Arguments (opts)

  :rating - Controlled component value for the rating. A value between 1 and 5

  :default-rating - Uncontrolled component value for the rating. A value between 1 and 5

  :color - The color styling of the rating component. `:primary`,
  `:white`, `:black`. [default: `:primary`]

  :on-change - Event callback function called when the rating of the
  given component changes. Function receives one parameter which
  consists of the rating between 1 and 5. (fn [rating]).

  :size - The size styling of the rating component. `:normal`,
  `:default`, `:large`, `:small`. [default: `:default`].
  "
  [{:keys [default-rating color size]
    :or {color :primary size :default}}]
  (let [*current-default-rating (r/atom default-rating)
        color-class (case color
                      :primary "primary"
                      :white   "white"
                      :black   "black")
        size-class (case size
                     :default  ""
                     :small    "small"
                     :large    "large")]
    (fn [{:keys [rating default-rating color on-change size]
          :or {color :primary size :default}}]
      (let [current-rating (if default-rating @*current-default-rating rating)]
        [:div.ethlance-component-rating
         {:class [color-class size-class]}
         (doall
          (for [i (range 1 6)]
            ^{:key i}
            [c-star {:active? (<= i current-rating)
                     :color color
                     :size size
                     :index i
                     :on-change
                     (when on-change
                       (fn [index]
                         (reset! *current-default-rating index)
                         (on-change index)))}]))]))))
