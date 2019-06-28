(ns ethlance.ui.component.rating
  "UI Component which displays 5 stars for displaying and updating user feedback."
  (:require
   [reagent.core :as r]
   [ethlance.ui.component.inline-svg :refer [c-inline-svg]]))


(def rating-star-src "images/icons/ethlance-star-icon.svg")


(defn handle-svg-ready [index on-change dom-ref inline-svg]
  (.addEventListener inline-svg "click" #(on-change index)))


(defn c-star []
  (fn [{:keys [active? color index on-change size]
        :or {color :primary size :default}}]
    (let [color-class (case color
                        :primary " primary "
                        :white   " white "
                        :black   " black ")
          size-class (case size
                        :normal " "
                        :default " "
                        :large " large "
                        :small " small ")
          size-value (case size
                        :normal 24
                        :default 24
                        :large 36
                        :small 18)
          active-class (when active? " active ")]
      [c-inline-svg {:key (str index)
                     :src rating-star-src
                     :on-ready (when on-change #(handle-svg-ready index on-change %1 %2))
                     :width size-value
                     :height size-value
                     :class (str " star " color-class size-class active-class)}])))


(defn c-rating [{:keys [rating color on-change size]
                 :or {color :primary rating 0 size :default}}]
  (let [*current-rating (r/atom rating)]
    (fn [{:keys [rating color on-change size]
          :or {color :primary rating 0 size :default}}]
      [:div.ethlance-component-rating
       (doall
        (for [i (range 1 6)]
          ^{:key i}
          [c-star {:active? (<= i @*current-rating)
                   :color color
                   :size size
                   :index i
                   :on-change
                   (when on-change
                     (fn [index]
                       (reset! *current-rating index)
                       (on-change index)))}]))])))
         
