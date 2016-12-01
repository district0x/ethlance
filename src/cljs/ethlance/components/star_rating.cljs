(ns ethlance.components.star-rating
  (:require [cljsjs.react-star-rating-component]
            [cljs-react-material-ui.icons :as icons]
            [reagent.core :as r]
            [cljs-react-material-ui.reagent :as ui]
            [ethlance.styles :as styles]))

(def star-rating* (r/adapt-react-class js/ReactStarRatingComponent))

(defn star-rating [props]
  [star-rating*
   (merge
     {:name "star-rating"
      :star-count 5
      :render-star-icon (fn [index value]
                          (if (and (<= (- index 0.5) value) (> index value))
                            (icons/toggle-star-half {:style styles/star-rating})
                            (if (<= index value)
                              (icons/toggle-star {:style styles/star-rating})
                              (icons/toggle-star-border {:style styles/star-rating}))))}
     props
     {:on-star-click (fn [next-val prev-val]
                       (let [next-val (if (= next-val prev-val)
                                        (dec next-val)
                                        next-val)]
                         (when-let [on-star-click (:on-star-click props)]
                           (on-star-click next-val prev-val))))})])

