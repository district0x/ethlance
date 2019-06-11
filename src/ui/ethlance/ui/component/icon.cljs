(ns ethlance.ui.component.icon
  (:require
   [ethlance.ui.component.inline-svg :refer [c-inline-svg]]))


(def icon-listing
  {:about {:src "images/icons/ethlance-about-icon.svg"}
   :arbiters {:src "images/icons/ethlance-arbiters-icon.svg"}
   :candidates {:src "images/icons/ethlance-candidates-icon.svg"}
   :jobs {:src "images/icons/ethlance-jobs-icon.svg"}
   :search {:src "images/icons/ethlance-search-icon.svg"}
   :sign-up {:src "images/icons/ethlance-sign-up-icon.svg"}
   :facebook {:src "images/icons/facebook-icon.svg"}
   :github {:src "images/icons/github-icon.svg"}
   :linkedin {:src "images/icons/linkedin-icon.svg"}
   :slack {:src "images/icons/slack-icon.svg"}
   :twitter {:src "images/icons/twitter-icon.svg"}
   :close {:src "images/svg/close.svg"}})


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
                           :x-small [8 8]
                           :small [16 16]
                           :normal [24 24]
                           :large [32 32])
          
          src (-> icon-listing name :src)
          style (-> icon-listing name :style)]
      [:div.ethlance-icon {:style (or style {})}
       [c-inline-svg {:src src
                      :width width
                      :height height
                      :class (str "ethlance-icon-svg " color)}]])))
