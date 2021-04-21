(ns ethlance.ui.component.carousel
  (:require [ethlance.ui.component.circle-button :refer [c-circle-icon-button]]
            [ethlance.ui.component.profile-image :refer [c-profile-image]]
            [ethlance.ui.component.rating :refer [c-rating]]
            [reagent.core :as r]
            ["pure-react-carousel" :as react-carousel]))

(defn c-carousel-old
  "Carousel Component for displaying multiple 'slides' of content

  # Keyword Arguments

  opts - Optional Arguments

  children - Each individual slide to be displayed within the carousel.

  # Optional Arguments (opts)

  :default-index - The index within the `children` to display
  first [default: 0]

  # Examples

  ```clojure
  [c-carousel {}
   [c-feedback-slide feedback-1]
   [c-feedback-slide feedback-2]]
  ```
  "
  [{:keys [default-index]
    :or {default-index 0}}]
  (let [*current-index (r/atom default-index)]
    (r/create-class
     {:display-name "ethlance-carousel"
      :reagent-render
      (fn [_ & children]
        (let [first-slide? (<= @*current-index 0)
              last-slide? (>= @*current-index (dec (count children)))]
          [:div.ethlance-carousel
           [:div.slide-listing
            (when-not first-slide?
              [:div.left-slide])
            [:div.current-slide
             (nth children @*current-index)]
            (when-not last-slide?
              [:div.right-slide])]
           [:div.button-listing
            [:div.back-button
             [c-circle-icon-button
              {:name :ic-arrow-left
               :hide? first-slide?
               :on-click #(swap! *current-index dec)}]]
            [:div.forward-button
             [c-circle-icon-button
              {:name :ic-arrow-right
               :hide? last-slide?
               :on-click #(swap! *current-index inc)}]]]]))})))

(defn c-feedback-slide
  [{:keys [id rating author text class]}]
  [:div.feedback-slide
   ;; FIXME: use better unique key
   {:key (str "feedback-" id "-" rating) :class class}
   [:div.profile-image [c-profile-image {}]]
   [:div.rating [c-rating {:rating rating :color :white}]]
   [:div.message text]
   [:div.name author]])

(defn c-carousel [{:keys []} & children]
  [:div.ethlance-new-carousel
   [:> react-carousel/CarouselProvider {:natural-slide-width 388
                                        :natural-slide-height 300
                                        :total-slides (count children)
                                        :visible-slides 1}
    [:div.slider-outer
     [:> react-carousel/Slider
      (for [[idx child] (map-indexed vector children)]
        ^{:key idx} [:> react-carousel/Slide {:index idx} child])]]

    [:div.back-button
     [:> react-carousel/ButtonBack {:className "ethlance-circle-button ethlance-circle-icon-button primary"}
      [c-circle-icon-button
       {:name :ic-arrow-left}]]]
    [:div.forward-button
     [:> react-carousel/ButtonNext {:className "ethlance-circle-button ethlance-circle-icon-button primary"}
      [c-circle-icon-button
       {:name :ic-arrow-right}]]]]])
