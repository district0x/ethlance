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
     (fn [{:keys [dimensions] :as opts} child]
       [:div.scrollable
        [:div.scroll-container
         child]
        [:div.scroll-bar-x]
        [:div.scroll-bar-y]])})))
  
