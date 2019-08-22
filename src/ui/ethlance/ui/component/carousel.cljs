(ns ethlance.ui.component.carousel
  (:require
   [reagent.core :as r]))


(defn c-carousel
  [{:keys [default-index]
    :or {default-index 0}
    :as opts} & children]
  (let [*current-index (r/atom default-index)]
    (r/create-class
     {:display-name "ethlance-carousel"
      :reagent-render
      (fn [opts & children]
        [:div.ethlance-carousel
         [:div.left-slide]
         [:div.back-button]
         [:div.current-slide
          (nth children @*current-index)]
         [:div.forward-button]
         [:div.right-slide]])})))
