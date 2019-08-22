(ns ethlance.ui.component.carousel
  (:require
   [reagent.core :as r]))


(defn c-carousel
  [{:keys [] :as opts} & children]
  (r/create-class
   {:display-name "ethlance-carousel"
    :reagent-render
    (fn [{:keys [] :as opts} & children]
      [:div.ethlance-carousel
       "ethlance carousel"])}))
