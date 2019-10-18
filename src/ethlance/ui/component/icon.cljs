(ns ethlance.ui.component.icon
  (:require
   [ethlance.ui.component.inline-svg :refer [c-inline-svg]]))


(def icon-listing
  {:about {:src "images/icons/ethlance-about-icon.svg"}
   :arbiters {:src "images/icons/ethlance-arbiters-icon.svg"}
   :candidates {:src "images/icons/ethlance-candidates-icon.svg"}
   :close {:src "images/svg/close.svg"}
   :facebook {:src "images/icons/facebook-icon.svg"}
   :github {:src "images/icons/github-icon.svg"}
   :ic-arrow-up {:src "images/svg/ic-arrow-up.svg"}
   :ic-arrow-down {:src "images/svg/ic-arrow-down.svg"}
   :ic-arrow-left {:src "images/svg/ic-arrow-left.svg"}
   :ic-arrow-left2 {:src "images/svg/ic-arrow-left2.svg"}
   :ic-arrow-right {:src "images/svg/ic-arrow-right.svg"}
   :ic-arrow-right2 {:src "images/svg/ic-arrow-right2.svg"}
   :ic-upload {:src "images/svg/ic-upload.svg"}
   :jobs {:src "images/icons/ethlance-jobs-icon.svg"}
   :linkedin {:src "images/icons/linkedin-icon.svg"}
   :list-menu {:src "images/svg/list-menu.svg"}
   :my-activity {:src "images/svg/my-activity.svg"}
   :search {:src "images/icons/ethlance-search-icon.svg"}
   :sign-up {:src "images/icons/ethlance-sign-up-icon.svg"}
   :slack {:src "images/icons/slack-icon.svg"}
   :twitter {:src "images/icons/twitter-icon.svg"}})


(defn c-icon
  "SVG Icon of common icons throughout the Ethlance website.

  # Keyword Arguments:

  props - Additonal optional arguments, along with React Props

  # Optional Arguments (props)

  :name - The name of the icon to use, as supplied by the
  `ethlance.ui.component.icon/icon-listing`. [default: `:about`]

  :color - The color of the SVG icon. `:primary`, `:secondary`,
  `:white`, `:black`, `:dark-blue`, `:none`. [default: `:primary`]

  :size - The size of the SVG icon. `:x-small`, `:small`, `:normal`, `:large`.
  
  # Notes

  - Additional React Props can be supplied to the `props` keyword
  argument.

  "
  []
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
                  :dark-blue "dark-blue"
                  :none "")

          [width height] (case size
                           :x-small [8 8]
                           :small [16 16]
                           :normal [24 24]
                           :large [32 32])
          
          src (-> icon-listing name :src)
          style (-> icon-listing name :style)]
      
      (assert src "Given 'name' parameter does not exist")
      [:div.ethlance-icon (merge props {:style (or style {})})
       [c-inline-svg {:src src
                      :width width
                      :height height
                      :class (str "ethlance-icon-svg " color)}]])))
