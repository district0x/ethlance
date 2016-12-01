(ns ethlance.components.slider-with-counter
  (:require [ethlance.styles :as styles]
            [cljs-react-material-ui.reagent :as ui]))

(defn slider-with-counter []
  (fn [slider-props counter-text]
    [:div
     [ui/slider (merge
                  {:slider-style styles/search-slider}
                  slider-props)]
     [:div {:style styles/slider-value} counter-text]]))
