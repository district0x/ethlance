(ns ethlance.styles
  (:require [cljs-react-material-ui.core :refer [color get-mui-theme]]))

(def primary1-color (color #_ :teal600 #_:blue600 #_:light-blue600 :indigo500))
(def logo-color (color :light-blue50))
(def accent1-color (color :pinkA200))
(def text-color "rgba(0, 0, 0, 0.87)")

(def palette {:primary1-color primary1-color
              :accent1-color accent1-color
              :text-color text-color})

(def mui-theme (get-mui-theme {:palette palette
                               :font-family "Open Sans, sans-serif"
                               :text-field {:error-color (color :pink400)}
                               :date-picker {:select-color primary1-color}}))

(def desktop-gutter (aget js/MaterialUIStyles "spacing" "desktopGutter"))
(def desktop-gutter-less (aget js/MaterialUIStyles "spacing" "desktopGutterLess"))
(def desktop-gutter-mini (aget js/MaterialUIStyles "spacing" "desktopGutterMini"))

(defn emphasize [clr]
  (js/MaterialUIUtils.colorManipulator.emphasize clr))

(defn lighten [clr coef]
  (js/MaterialUIUtils.colorManipulator.lighten clr coef))

(defn darken [clr coef]
  (js/MaterialUIUtils.colorManipulator.darken clr coef))

(def app-bar-left
  {:background-color primary1-color})

(def app-bar-right
  {:background-color primary1-color})

(def nav-list
  {:padding-top 0})

(def fade-color
  "rgba(0, 0, 0, 0.45)"
  #_(aget mui-theme "subheader" "color"))

(def fader-color
  "rgba(0, 0, 0, 0.35)")

(def fade-text
  {:color fade-color})

(def fader-text
  {:color "rgba(0, 0, 0, 0.35)"})

(def dark-text
  {:color (color :black)})

(def white-text
  {:color (color :white)})

(def app-bar-select-field-label
  {:color "#FFF"})

(def text-left
  {:text-align :left})

(def text-right
  {:text-align :right})

(def text-center
  {:text-align :center})

(defn margin-horizontal [x]
  {:margin-right x
   :margin-left x})

(defn margin-vertical [x]
  {:margin-top x
   :margin-bottom x})

(defn padding-horizontal [x]
  {:padding-right x
   :padding-left x})

(defn padding-vertical [x]
  {:padding-top x
   :padding-bottom x})

(defn padding-all [x]
  {:padding-top x
   :padding-bottom x
   :padding-right x
   :padding-left x})


(def full-width
  {:width "100%"})

(def full-height
  {:height "100%"})

(def content-wrap
  (padding-all desktop-gutter))

(def paper-secton-thin
  {:padding desktop-gutter-less
   :margin-bottom desktop-gutter-less})

(def paper-secton-mini
  {:padding desktop-gutter-mini
   :margin-bottom desktop-gutter-mini})

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
  {:color (color :amberA400) #_primary1-color})

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

(def position-relative
  {:position :relative})

(def feedback-style-rating
  {:margin-bottom 3})

(def navigation-drawer
  {:height "100%"
   :flex-direction "column"
   :display "flex"
   :justify-content "space-between"})

(def search-result-headline
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
   :text-align :center
   :color primary1-color
   :font-style :italic})

(def last-transaction-info
  {:color "rgba(0, 0, 0, 0.7)"
   :position :absolute
   :bottom 10
   :left 10
   :text-align :center
   :font-size "0.85em"})

(def social-button
  {:margin-right 12})

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
  {:width "100%"
   :border-top "1px solid #e0e0e0"
   :margin-top desktop-gutter
   :margin-bottom desktop-gutter})

(def hr-small
  (merge
    hr
    {:margin-top desktop-gutter-less
     :margin-bottom desktop-gutter-less}))

(def no-box-shadow
  {:box-shadow "none"})

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
         {:margin-bottom 10
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

(def display-inline
  {:display :inline})

(def display-block
  {:display :block})

(def user-detail-h2-line
  {:display :inline
   :margin-left "5px"})

(def paper-title
  )

(def allow-whitespaces
  {:white-space :pre-wrap})

(def success-color (color :green500))
(def danger-color (color :red500))
(def pending-color (color :yellow800))

(def job-status-colors
  {1 success-color
   2 (color :red800)})

(def job-payment-type-colors
  {1 (color :blue500)
   2 (color :cyan500)
   3 (color :teal500)})

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
  {1 (color :amber700)
   2 (color :amber900)})

(def contract-status-colors
  {1 (color :deep-purple300)
   2 pending-color
   3 success-color
   4 (color :red800)
   5 (color :grey500)})

(def freelancers-needed-status-color (color :deep-orange500))

(def invoice-status-colors
  {1 pending-color
   2 success-color
   3 danger-color})

(def freelancer-available?-color
  {true success-color
   false danger-color})

(def budget-chip-color
  (color :brown500))

(def skills-chip-color
  (color :deep-purple50))

(def languages-chip-color
  (color :green100))

(def categories-chip-color
  (color :orange100))

(def line
  {:line-height "1.3em"})

(def pagination-button
  {:min-width 36})

(def pagination-row-column
  {:padding-right 0
   :padding-left 0
   :overflow :auto})

(def word-wrap-break
  {:word-wrap :break-word})

(def message-bubble
  {:white-space :pre-wrap
   :border-radius "12px"
   :word-wrap :break-word
   :padding desktop-gutter-less})

(def message-bubble-row
  {:margin-bottom desktop-gutter-less
   :font-size "0.95em"})

(def message-bubble-right
  (merge message-bubble
         {:background-color primary1-color
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
         {:max-height 50
          :margin-top 2}))

(def table-highlighted-row
  {:background-color (color :indigo50)})

(def app-bar-balance
  (merge white-text
         {:margin-right 20}))

(def app-bar-user
  (merge white-text
         {:margin-right 10}))

(def app-bar-user-name
  (merge white-text
         {:margin-right 5}))

(def app-bar-loading-users
  (merge white-text
         {:font-weight 300}))

(def landing-bg
  {:margin-top 64
   :width "100%"
   :height "700px"
   :background-color primary1-color})

(def landing-banner
  {:width "860px"
   :margin-bottom "-60px"
   :padding desktop-gutter-less
   :z-index 99})

(def landing-menu-title
  {:font-size "16px"
   :font-weight 300
   ;:margin-left 10
   ;:width "100%"
   :text-align "center"})

(def landing-menu-icon-smaller
  {:max-height "90px"})

(def diagram-icon
  {:max-height "90px"})

(def diagram-arrow-icon
  {:max-height "80px"})

(def footer-subheader
  {:color "#a3a3a3"
   :padding-left 0})

(def ethlance-logo
  {:font-family "BlendaScript"
   :color logo-color
   :font-size "35px"
   :margin-top 4
   :display :block})

(def ethlance-logo-footer
  (merge ethlance-logo
         {:color "rgba(255, 255, 255, 0.75)"
          :font-size "30px"
          :margin-top 10}))

(def landing-title-base
  {:color "#FFF"})

(def landing-title
  (merge landing-title-base
         {:font-size 55}))

(def landing-subtitle
  (merge landing-title-base
         {:font-size 25}))

(def landing-app-bar
  {:position :fixed
   :box-shadow "none"
   :top 0})

(def landing-button
  {:background-color :transparent
   :box-shadow "none"
   :border "1px solid #FFF"
   :width 200
   :margin "2px 10px"})

(def landing-feature-seaction
  {:width "100%"
   :height "725px"})

(def feature-no-cut
  (merge landing-feature-seaction
         {:background-color "#bbdefb"
          :padding-top 50}))

(def feature-blockchain
  (merge landing-feature-seaction
         {:padding-bottom "50px"
          :background-color "#FFF"}))

(def feature-no-restrictions
  (merge landing-feature-seaction
         {:background-color "#ff8a80"}))

(def process-diagram
  {:background-color "#FFF"
   :padding "36px 0"})

(def landing-feature-image
  {:width "100%"
   :max-height "550px"
   })

(def table-row-column-thin
  {:padding-left 3
   :padding-right 3})

(def detail-action-button
  {:margin-top 2
   :margin-bottom 2})

(def table-status-chip
  {:margin-right 0
   :margin-bottom 0
   :float :left})

(def job-status-chip
  {:margin-right 5
   :margin-bottom 5})

(def freelancer-search-result-info-row
  (merge fade-text
         full-width
         {:margin-top 5
          :margin-bottom 5}))

(def user-forms-text-field
  {:width 300})

(def chip-input-menu-props
  {:max-height 300})

(def ether-field-currency
  {:font-size "1.2em"
   :margin-left 5
   :margin-bottom 9
   :font-weight 300})

(def feedback-form-star-numbers
  {:font-size "1.1em"
   :margin-bottom 5
   :margin-left 5})

(def skills-input-xs-hint-size
  {:font-size "11px"})

(def gravatar-hint
  {:padding-left 10
   :font-size 12
   :width 150
   :text-align :center})