(ns ethlance.ui.component.pagination
  (:require
   [reagent.core :as r]
   [re-frame.core :as re]
   [district.parsers :refer [parse-int]]

   ;; Ethlance Components
   [ethlance.ui.component.icon :refer [c-icon]]))


;; Math Functions
(def ceil (aget js/Math "ceil"))
(def floor (aget js/Math "floor"))
(def abs (aget js/Math "abs"))


(defn c-pagination
  "Component for handling pagination wrt a given listing.

  "
  []
  (fn [{:keys [total-count
               has-next-page?
               limit
               offset
               set-offset-event]}]
    (let [current-page (-> offset (/ limit) ceil inc)
          num-pages (-> total-count (/ limit) ceil inc)
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
