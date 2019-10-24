(ns ethlance.ui.component.scrollable
  (:require
   [reagent.core :as r]))


(defn c-scrollable
  [opts child]
  (let []
    (r/create-class
     {:display-name "c-scrollable"

      :component-did-mount
      (fn [this])
      
      :reagent-render
      (fn [{:keys [fixed-width? fixed-height? color]
            :or {fixed-width? true
                 fixed-height? true
                 color :primary}
            :as opts}
           child]
        ;; TODO: for dynamic situations, remove scrollable functionally when both are true instead.
        (assert (or fixed-width? fixed-height?) "At least `fixed-width? or fixed-height? must be true")
        (let [color-class (case color
                            :primary " primary "
                            :secondary " secondary "
                            "")]
          [:div.scrollable
           {:class [(when fixed-width? " fixed-width ")
                    (when fixed-height? " fixed-height ")
                    color-class]}
           [:div.scroll-container
            child]
           [:div.scroll-bar-x]
           [:div.scroll-bar-y]]))})))
