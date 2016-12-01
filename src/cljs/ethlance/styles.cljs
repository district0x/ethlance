(ns ethlance.styles
  (:require [cljs-react-material-ui.core :refer [color get-mui-theme]]))

(def primary1-color (color :indigo500))

(def palette {:primary1-color primary1-color})

(def mui-theme (get-mui-theme {:palette palette}))

(def desktop-gutter (aget js/MaterialUIStyles "spacing" "desktopGutter"))
(def desktop-gutter-less (aget js/MaterialUIStyles "spacing" "desktopGutterLess"))

(def app-bar-left
  {:background-color primary1-color})

(def app-bar-right
  {:background-color primary1-color})

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
  {:color primary1-color})

(def job-title
  {:color primary1-color
   :font-weight 500})

(def job-info
  (merge fade-text
    {:font-weight 600
     :font-size "0.9em"}))

(def job-description
  {})

(def more-text
  {:color primary1-color
   :cursor :pointer})

(def row-no-margin
  {:margin-left 0
   :margin-right 0})

(def skill-chips-row
  (merge
    row-no-margin
    {:padding-top 5
     :padding-bottom 5}))

(def skill-chip
  {:margin-left 3
   :margin-right 3})