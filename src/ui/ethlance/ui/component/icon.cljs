(ns ethlance.ui.component.icon
  (:require
   [ethlance.ui.component.inline-svg :refer [c-inline-svg]]))


(def icon-listing
  {:about {:src "images/vector_logo/ethlance-about-icon.svg"
           :style {}}})


(defn c-icon []
  (fn [{:keys [name color size]
        :or {name :about
             color :primary
             size :normal}
        :as props}]
    (let [props (dissoc props :name :color :size)
          
          color (case color
                  :primary "primary"
                  :secondary "secondary"
                  :white "white"
                  :black "black"
                  :none "")

          [width height] (case size
                           :small [16 16]
                           :normal [24 24]
                           :large [32 32])
          
          src (-> icon-listing name :src)
          style (-> icon-listing name :style)]
      [:div.ethlance-icon {:style style}
       [c-inline-svg {:src src
                      :width width
                      :height height
                      :class (str "ethlance-icon-svg " color)}]])))
