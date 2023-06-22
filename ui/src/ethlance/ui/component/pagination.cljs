(ns ethlance.ui.component.pagination
  (:require [ethlance.ui.component.icon :refer [c-icon]]
            [ethlance.ui.component.circle-button :refer [c-circle-icon-button]]
            [re-frame.core :as re]))

;; Math Functions
(def ceil (aget js/Math "ceil"))

(defn c-pagination
  "Component for handling pagination wrt a given listing.

  Keyword Arguments:

  total-count - Total count of the listing

  limit - The current limit of the listing

  offset - The current offset of the listing

  set-offset-event - Event to dispatch to change the offset value.
  "
  []
  (fn [{:keys [total-count
               limit
               offset
               set-offset-event]}]
    (let [total-count (or total-count 0)
          current-page (-> offset (/ limit) ceil inc)
          num-pages (-> total-count (/ limit) ceil)
          prev-offset (- offset limit)
          prev-offset (if (< prev-offset 0) 0 prev-offset)
          next-offset (+ offset limit)
          next-offset (if (>= next-offset total-count) offset next-offset)]
      [:div.pagination
       [c-icon
        {:name :ic-arrow-left
         :class [(when (= offset 0) "disabled")]
         :color :secondary
         :inline? false
         :title "Go To Previous Page"
         :on-click #(re/dispatch [set-offset-event prev-offset])}]
       [:div.range-label
        [:span.current-page current-page]
        [:span.of "of"]
        [:span.num-pages num-pages]]
       [c-icon
        {:name :ic-arrow-right
         :class [(when (>= (+ offset limit) total-count) "disabled")]
         :color :secondary
         :inline? false
         :title "Go To Next Page"
         :on-click #(re/dispatch [set-offset-event next-offset])}]])))

(defn c-pagination-ends
  "Component for handling pagination with <first,prev,next,final>

  Keyword Arguments:

  total-count - Total count of the listing

  limit - The current limit of the listing

  offset - The current offset of the listing

  set-offset-event - Event to dispatch to change the offset value.
  "
  []
  (fn [{:keys [total-count
               limit
               offset
               set-offset-event]}]
    (let [total-count (or total-count 0)
          current-page (-> offset (/ limit) ceil inc)
          num-pages (-> total-count (/ limit) ceil)
          prev-offset (- offset limit)
          prev-offset (if (< prev-offset 0) 0 prev-offset)
          next-offset (+ offset limit)
          next-offset (if (>= next-offset total-count) offset next-offset)
          first-offset 0
          last-offset (* (- num-pages 1) limit)]
      [:div.button-listing
       [c-circle-icon-button
        {:name :ic-arrow-left2
         :disabled? (= offset 0)
         :size :small
         :on-click #(re/dispatch [set-offset-event first-offset])}]
       [c-circle-icon-button
        {:name :ic-arrow-left
         :disabled? (= offset 0)
         :size :small
         :on-click #(re/dispatch [set-offset-event prev-offset])}]
       [c-circle-icon-button
        {:name :ic-arrow-right
         :disabled? (>= (+ offset limit) total-count)
         :size :small
         :on-click #(re/dispatch [set-offset-event next-offset])}]
       [c-circle-icon-button
        {:name :ic-arrow-right2
         :disabled? (>= (+ offset limit) total-count)
         :size :small
         :on-click #(re/dispatch [set-offset-event last-offset])}]])))
