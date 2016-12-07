(ns ethlance.components.star-rating
  (:require [cljs-react-material-ui.icons :as icons]
            [reagent.core :as r]
            [cljs-react-material-ui.reagent :as ui]
            [ethlance.components.misc :refer [col row row-plain]]
            [ethlance.styles :as styles]
            [ethlance.utils :as u]))

(defn star-rating []
  (fn [{:keys [value star-count star-style on-star-click display-number? small? rating-number-style]
        :or {star-count 5}
        :as props}]
    [row-plain
     (merge
       {:middle "xs"}
       (dissoc props :value :star-count :star-style :on-star-click :display-number? :small?
               :rating-number-style))
     (for [i (range 1 (inc star-count))]
       (let [star-props {:key i
                         :style (assoc (merge styles/star-rating
                                              (when small? styles/star-rating-small)
                                              star-style)
                                  :cursor (if on-star-click :pointer :auto))
                         :on-touch-tap (fn []
                                         (let [next-val (if (= i value) (dec i) i)]
                                           (when on-star-click
                                             (on-star-click next-val))))}]
         (if (and (<= (- i 0.5) value) (> i value))
           (icons/toggle-star-half star-props)
           (if (<= i value)
             (icons/toggle-star star-props)
             (icons/toggle-star-border star-props)))))
     (when display-number?
       [:h4 {:style (merge
                      styles/star-rating-number
                      (when small? styles/star-rating-number-small)
                      rating-number-style)} (u/round value)])]))
