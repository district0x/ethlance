(ns ethlance.ui.component.profile-image
  (:require
   [reagent.core :as r]))


(defn c-profile-image
  [{:keys [src size] :as opts}]
  (let [size-class (case size
                    :small " small "
                    :normal ""
                    :large " large "
                    "")]
    [:div.ethlance-profile-image
     {:class size-class}
     [:img {:src src}]]))
