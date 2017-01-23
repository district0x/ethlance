(ns ethlance.components.star-rating
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.icons :as icons]
    [ethlance.components.misc :refer [col row row-plain]]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [reagent.core :as r]))

(defn star-rating []
  (fn [{:keys [value star-count star-style on-star-click show-number? small? rating-number-style ratings-count]
        :or {star-count 5}
        :as props}]
    (let [star-coef (/ star-count 5)
          value (* star-coef value)]
      [row-plain
       (r/merge-props
         {:middle "xs"}
         (dissoc props :value :star-count :star-style :on-star-click :show-number? :small?
                 :rating-number-style :ratings-count))
       (for [i (range 1 (inc star-count))]
         (let [star-props {:key i
                           :style (assoc (merge styles/star-rating
                                                (when small? styles/star-rating-small)
                                                star-style)
                                    :cursor (if on-star-click :pointer :auto))
                           :on-touch-tap (fn []
                                           (let [next-val (if (= i value) (dec i) i)]
                                             (when on-star-click
                                               (on-star-click (/ next-val star-coef)))))}]
           (if (and (<= (- i 0.5) value) (> i value))
             (icons/star-half star-props)
             (if (<= i value)
               (icons/star star-props)
               (icons/star-outline star-props)))))
       (when show-number?
         [:h4
          {:style (merge
                    styles/star-rating-number
                    (when small? styles/star-rating-number-small)
                    rating-number-style)}
          (u/round value)
          (when ratings-count
            (str " from " ratings-count " " (u/pluralize "feedback" ratings-count)))])])))
