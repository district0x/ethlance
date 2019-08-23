(ns ethlance.ui.component.carousel
  (:require
   [reagent.core :as r]
   
   ;; Ethlance Components
   [ethlance.ui.component.button :refer [c-button c-button-icon-label c-circle-icon-button]]
   [ethlance.ui.component.circle-button :refer [c-circle-icon-button]]))


(defn c-carousel
  [{:keys [default-index]
    :or {default-index 0}
    :as opts} & children]
  (let [*current-index (r/atom default-index)]
    (r/create-class
     {:display-name "ethlance-carousel"
      :reagent-render
      (fn [opts & children]
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
