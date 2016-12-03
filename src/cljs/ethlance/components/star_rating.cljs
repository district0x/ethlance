(ns ethlance.components.star-rating
  (:require [cljs-react-material-ui.icons :as icons]
            [reagent.core :as r]
            [cljs-react-material-ui.reagent :as ui]
            [ethlance.components.misc :refer [col row row-plain]]
            [ethlance.styles :as styles]))

(defn star-rating []
  (fn [{:keys [value star-count star-style on-star-click display-number?]
        :or {star-count 5}
        :as props}]
    [row-plain
     (dissoc props :value :star-count :star-style :on-star-click :display-number?)
     (for [i (range 1 (inc star-count))]
       (let [star-props {:key i
                         :style (assoc (merge styles/star-rating star-style)
                                  :cursor (if on-star-click :pointer :auto))
                         :on-touch-tap (fn []
                                         (let [next-val (if (= i value) (dec i) i)]
                                           (when on-star-click
                                             (on-star-click next-val))))}]
         (if (and (<= (- i 0.5) value) (> i value))
           (icons/toggle-star-half star-props)
           (if (<= i value)
             (icons/toggle-star star-props)
             (icons/toggle-star-border star-props)))))]))
