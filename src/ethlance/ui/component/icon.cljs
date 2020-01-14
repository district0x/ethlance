(ns ethlance.ui.component.icon
  (:require
   [ethlance.ui.component.inline-svg :refer [c-inline-svg]]))


(def icon-listing
  {:about {:src "/images/icons/ethlance-about-icon.svg"}
   :arbiters {:src "/images/icons/ethlance-arbiters-icon.svg"}
   :candidates {:src "/images/icons/ethlance-candidates-icon.svg"}
   :close {:src {:default "/images/svg/close.svg"
                 :white "/images/svg/close-white.svg"
                 :black "/images/svg/close-black.svg"}}
   :facebook {:src "/images/icons/facebook-icon.svg"}
   :github {:src {:default "/images/icons/github-icon.svg"
                  :white "/images/icons/github-icon-white.svg"}}
   :ic-arrow-up {:src {:default "/images/svg/ic-arrow-up.svg"
                       :primary "/images/svg/ic-arrow-up-primary.svg"
                       :secondary "/images/svg/ic-arrow-up-secondary.svg"}}
   :ic-arrow-down {:src {:default "/images/svg/ic-arrow-down.svg"
                         :primary "/images/svg/ic-arrow-down-primary.svg"
                         :secondary "/images/svg/ic-arrow-down-secondary.svg"}}
   :ic-arrow-left {:src "/images/svg/ic-arrow-left.svg"}
   :ic-arrow-left2 {:src "/images/svg/ic-arrow-left2.svg"}
   :ic-arrow-right {:src "/images/svg/ic-arrow-right.svg"}
   :ic-arrow-right2 {:src "/images/svg/ic-arrow-right2.svg"}
   :ic-upload {:src {:default "/images/svg/ic-upload.svg"
                     :primary "/images/svg/ic-upload-primary.svg"}}
   :jobs {:src "/images/icons/ethlance-jobs-icon.svg"}
   :linkedin {:src {:default "/images/icons/linkedin-icon.svg"
                    :white "/images/icons/linkedin-icon-white.svg"}}
   :list-menu {:src "/images/svg/list-menu.svg"}
   :my-activity {:src {:default "/images/svg/my-activity.svg"
                       :primary "/images/svg/my-activity-primary.svg"
                       :black "/images/svg/my-activity.svg"
                       :white "/images/svg/my-activity-white.svg"}}
   :search {:src "/images/icons/ethlance-search-icon.svg"}
   :sign-up {:src "/images/icons/ethlance-sign-up-icon.svg"}
   :slack {:src "/images/icons/slack-icon.svg"}
   :twitter {:src "/images/icons/twitter-icon.svg"}})


(defn- icon-src
  [name color]
  (when-let [icon-attr (get-in icon-listing [name :src])]
    (cond
      (map? icon-attr)
      (or (get icon-attr color) (get icon-attr :default))

      :else
      icon-attr)))


(defn c-icon
  "SVG Icon of common icons throughout the Ethlance website.

  # Keyword Arguments:

  props - Additonal optional arguments, along with React Props

  # Optional Arguments (props)

  :name - The name of the icon to use, as supplied by the
  `ethlance.ui.component.icon/icon-listing`. [default: `:about`]

  :color - The color of the SVG icon. `:primary`, `:secondary`,
  `:white`, `:black`, `:dark-blue`, `:none`. [default: `:primary`]

  :size - The size of the SVG icon. `:x-small`, `:small`, `:normal`,
  `:large`. [default: `:normal`]
  
  :inline? - If true, the given SVG icon will be inlined within the
  DOM. [default: `true`]

  # Notes

  - Additional React Props can be supplied to the `props` keyword
  argument.

  - Colors are determined by the `icon-listing`, which can contain
  either an original source, or a listing of similar SVGs which
  consist of different colors.

  "
  []
  (fn [{:keys [name color size inline?]
        :or {name :about
             color :primary
             size :normal
             inline? true}
        :as props}]
    (let [props (dissoc props :name :color :size :inline?)
          
          color-class (case color
                        :primary "primary"
                        :secondary "secondary"
                        :white "white"
                        :black "black"
                        :dark-blue "dark-blue"
                        :none "")

          [width height] (case size
                           :x-small [8 8]
                           :smaller [12 12]
                           :small [16 16]
                           :normal [24 24]
                           :large [32 32])
          
          src (icon-src name color)
          style (-> icon-listing name :style)]
      
      (assert src (str "Given icon does not exist. Name: " name " Color: " color))
      [:div.ethlance-icon (merge props {:style (or style {})})
       (if-not inline?
         [:img {:src src
                :style {:width (str width "px")
                        :height (str height "px")}}]
         [c-inline-svg {:src src
                        :width width
                        :height height
                        :class (str "ethlance-icon-svg " color-class)}])])))
