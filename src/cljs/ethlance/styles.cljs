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

(def fade-color
  "rgba(0, 0, 0, 0.45)"
  #_(aget mui-theme "subheader" "color"))

(def fade-text
  {:color fade-color})

(def fader-text
  {:color "rgba(0, 0, 0, 0.35)"})

(def dark-text
  {:color (color :black)})

(def white-text
  {:color (color :white)})

(def address-select-field-label
  {:color "#FFF"})

(def text-left
  {:text-align :left})

(def text-right
  {:text-align :right})

(def text-center
  {:text-align :center})

(def full-width
  {:width "100%"})

(def content-wrap
  {:padding desktop-gutter
   :padding-left (+ 256 desktop-gutter)})

(def paper-secton-thin
  {:padding desktop-gutter-less
   :margin-bottom desktop-gutter-less})

(def paper-secton
  {:padding desktop-gutter
   :margin-bottom desktop-gutter})

(def margin-bottom-gutter
  {:margin-bottom desktop-gutter})

(def margin-bottom-gutter-less
  {:margin-bottom desktop-gutter-less})

(def margin-top-gutter
  {:margin-top desktop-gutter})

(def margin-top-gutter-more
  {:margin-top (* desktop-gutter 2)})

(def margin-top-gutter-less
  {:margin-top desktop-gutter-less})


(def paper-section-main
  {:min-height 300})

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

(def star-rating-number
  {:color primary1-color
   :font-size "1em"
   :margin-left 6})

(def star-rating-number-small
  {:margin-left 3
   :font-size "0.95em"})

(def star-rating-small
  {:width 18
   :height 18})

(def feedback-style-rating
  {:margin-bottom 3})

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

(def overflow-ellipsis
  {:overflow :hidden
   :text-overflow :ellipsis})

(def more-text
  {:cursor :pointer
   :font-style :italic})

(def clickable
  {:cursor :pointer})

(def row-no-margin
  {:margin-left 0
   :margin-right 0})

(def chip-list-row
  (merge
    row-no-margin
    {:padding-top 5
     :padding-bottom 5
     :min-height 45}))

(def chip-in-list
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

(def bold-fader-text
  (merge {:font-weight 600}
         fader-text))

(def message-bubble-time
  (merge bold-fader-text
         {:margin-bottom 10}))

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

(def display-inline
  {:display :inline})

(def display-block
  {:display :block})

(def user-detail-h2-line
  {:display :inline
   :margin-left "5px"})

(def paper-title
  )

(def detail-description
  {:white-space :pre-wrap})

(def success-color (color :green500))
(def danger-color (color :red500))
(def pending-color (color :yellow800))

(def job-status-colors
  {1 success-color
   2 danger-color})

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

(def contract-status-colors
  {1 (color :deep-purple300)
   2 pending-color
   3 success-color
   4 (color :red800)})                                      ;; change this

(def invoice-status-colors
  {1 pending-color
   2 success-color
   3 danger-color})

(def freelancer-available?-color
  {true success-color
   false danger-color})

(def budget-chip-color
  (color :brown500))

(def line
  {:line-height "1.2em"})

(def pagination-button
  {:min-width 36})

(def message-bubble
  {:white-space :pre-wrap
   :border-radius "12px"
   :word-wrap :break-word
   :padding desktop-gutter-less})

(def message-bubble-row
  {:margin-bottom desktop-gutter-less})

(def message-bubble-right
  (merge message-bubble
         {:background-color "#407fff"
          :color "#FFF"}))

(def message-bubble-left
  (merge message-bubble
         {:background-color "#f1f0f0"}))

(def italic-text
  {:font-style :italic})

(def contract-activity-row
  {:margin-bottom (* 2 desktop-gutter)})

(def form-item
  {:margin-top desktop-gutter-less})

(def profile-picture-name
  (merge overflow-ellipsis
         text-center
         {:max-height 50}))

(def table-highlighted-row
  {:background-color (color :indigo50)})