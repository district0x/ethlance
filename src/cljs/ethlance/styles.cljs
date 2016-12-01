(ns ethlance.styles
  (:require [cljs-react-material-ui.core :refer [color get-mui-theme]]))

(def palette {:primary1-color (color :indigo500)})

(def mui-theme (get-mui-theme {:palette palette}))

(def desktop-gutter (aget js/MaterialUIStyles "spacing" "desktopGutter"))
(def desktop-gutter-less (aget js/MaterialUIStyles "spacing" "desktopGutterLess"))

(def app-bar-left
  {:background-color (:primary1-color palette)})

(def app-bar-right
  {:background-color (:primary1-color palette)})

(def nav-list
  {:padding-top 0})

(def fade-text
  {:color (aget mui-theme "subheader" "color")})

(def address-select-field-label
  {:color "#FFF"})

(def content-wrap
  {:padding desktop-gutter
   :padding-left (+ 256 desktop-gutter)})

(def paper-secton
  {:padding desktop-gutter-less
   :margin-bottom desktop-gutter-less})

(def subheader
  {:padding-left 0})

(def search-slider
  {:margin-top 0
   :margin-bottom 0})

(def slider-value
  (merge
    fade-text
    {:text-align :right}))

(def star-rating
  {:color (:primary1-color palette)})