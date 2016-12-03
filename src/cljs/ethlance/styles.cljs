(ns ethlance.styles
  (:require [cljs-react-material-ui.core :refer [color get-mui-theme]]))

(def primary1-color (color :indigo500))

(def palette {:primary1-color primary1-color})

(def mui-theme (get-mui-theme {:palette palette}))

(def desktop-gutter (aget js/MaterialUIStyles "spacing" "desktopGutter"))
(def desktop-gutter-less (aget js/MaterialUIStyles "spacing" "desktopGutterLess"))

(def green-chip-color (color :green500))
(def red-chip-color (color :red500))

(def app-bar-left
  {:background-color primary1-color})

(def app-bar-right
  {:background-color primary1-color})

(def nav-list
  {:padding-top 0})

(def fade-color
  "rgba(0, 0, 0, 0.45)"
  #_ (aget mui-theme "subheader" "color"))

(def fade-text
  {:color fade-color})

(def dark-text
  {:color (color :black)})

(def address-select-field-label
  {:color "#FFF"})

(def text-left
  {:text-align :left})

(def content-wrap
  {:padding desktop-gutter
   :padding-left (+ 256 desktop-gutter)})

(def paper-secton-thin
  {:padding desktop-gutter-less
   :margin-bottom desktop-gutter-less})

(def paper-secton
  {:padding desktop-gutter
   :margin-bottom desktop-gutter})

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

(def star-rating-small
  {:width 18
   :height 18})

(def primary-text
  {:color primary1-color
   :font-weight 500})

(def job-info
  (merge fade-text
         {:font-weight 600
          :font-size "0.9em"
          :margin-top 5
          :margin-bottom 5}))

(def job-list-description
  {:overflow :hidden
   :margin-bottom 5})

(def job-list-title
  {:overflow :auto
   :text-overflow :ellipsis})

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
     :padding-bottom 5
     :min-height 45}))

(def skill-chip
  {:margin-left 3
   :margin-right 3
   :margin-bottom 3})

(def hr
  {:border-top "1px solid #e0e0e0"
   :margin-top desktop-gutter
   :margin-bottom desktop-gutter})

(def hr-small
  (merge
    hr
    {:margin-top desktop-gutter-less
     :margin-bottom desktop-gutter-less}))

(def employer-info
  (merge
    fade-text
    {:font-weight 600
     :font-size "0.9em"}))

(def employer-info-wrap
  {:min-height 24})

(def employer-rating-search
  {:margin-left 7})

(def employer-info-item
  {:margin-left 7})

(def freelancer-info-item
  {:margin-left 10})

(def location-icon-small
  {:width 18})

(def job-detail-title
  {:word-break :break-word})

(def paper-title
  )

(def detail-description
  {:white-space :pre-wrap})

(def job-status-colors
  {1 green-chip-color
   2 red-chip-color})

(def job-payment-type-colors
  {1 (color :blue500)
   2 (color :cyan500)})

(def job-estimation-duration-colors
  {1 (color :pink300)
   2 (color :pink500)
   3 (color :pink700)
   4 (color :pink900)})

(def job-experience-level-colors
  {1 (color :deep-purple300)
   2 (color :deep-purple500)
   3 (color :deep-purple900)})

(def job-hours-per-week-colors
  {1 (color :amber500)
   2 (color :amber800)})

(def budget-chip-color
  (color :brown500))

(def line
  {:line-height "1.2em"})