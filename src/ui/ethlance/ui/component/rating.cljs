(ns ethlance.ui.component.rating
  "UI Component which displays 5 stars for displaying and updating user feedback."
  (:require
   [reagent.core :as r]
   [ethlance.ui.component.inline-svg :refer [c-inline-svg]]))


(def rating-star-src "images/icons/ethlance-star-icon.svg")


(defn c-star []
  (fn [{:keys [active? color index on-change]
        :or {color :primary}}]
    (let [color-class (case color
                        :primary " primary "
                        :white   " white "
                        :black   " black ")
          
          active-class (when active? " active ")]
      [c-inline-svg {:src rating-star-src
                     :width 24
                     :height 24
                     :class (str " star " color-class active-class)}])))


(defn c-rating [{:keys [rating color on-change]
                 :or {color :primary rating 0}}]
  (let [current-rating (r/atom rating)]
    (fn [{:keys [rating color on-change]
          :or {color :primary rating 0}}]
      (let []
        [:div.ethlance-component-rating
         (for [i (range 1 6)]
           [c-star {:color color :active? (<= i @current-rating) :index i
                    :on-change on-change}])]))))
