(ns ethlance.components.search-results
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.icons :as icons]
    [ethlance.components.misc :as misc]
    [ethlance.components.misc :refer [col row row-plain currency a paper-thin]]
    [ethlance.components.star-rating :refer [star-rating]]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [re-frame.core :refer [subscribe dispatch]]))

(defn search-results-employer [{:keys [:user :show-balance?]}]
  (let [{:keys [:employer/jobs-count :employer/avg-rating :employer/total-paid
                :user/name :user/id :employer/ratings-count :user/country :user/state
                :user/balance]} user]
    [:div {:style styles/employer-info-wrap}
     (when (seq name)
       [row-plain
        {:middle "xs"
         :style styles/employer-info}
        [:span [a {:route :employer/detail
                   :route-params {:user/id id}
                   :style {:color styles/primary1-color}} name]]
        [star-rating
         {:value (u/rating->star avg-rating)
          :small? true
          :style styles/employer-rating-search}]
        [:span
         {:style styles/employer-info-item}
         [:span {:style styles/dark-text} ratings-count]
         (u/pluralize " feedback" ratings-count)]
        [:span
         {:style styles/employer-info-item}
         [:span {:style styles/dark-text} [currency total-paid]] " spent"]
        (when show-balance?
          [:span
           {:style styles/employer-info-item}
           [:span {:style styles/dark-text} [currency balance]] " balance"])
        [misc/country-marker
         {:country country
          :state state
          :row-props {:style styles/employer-info-item}}]])]))

(defn search-paper-thin []
  (let [xs-sm-width? (subscribe [:window/xs-sm-width?])]
    (fn [& children]
      (into
        [paper-thin
         {:style (if @xs-sm-width? styles/no-box-shadow {})}]
        children))))

(defn search-results [{:keys [:items-count :loading? :offset :limit :no-items-found-text :no-more-items-text
                              :next-button-text :prev-button-text :on-page-change]} body]
  [paper-thin
   {:loading? loading?}
   (if (pos? items-count)
     body
     [row {:center "xs" :middle "xs"
           :style {:min-height 200}}
      (when-not loading?
        (if (zero? offset)
          [:div no-items-found-text]
          [:div no-more-items-text]))])
   [row-plain {:end "xs"}
    (when (pos? offset)
      [ui/flat-button
       {:secondary true
        :label prev-button-text
        :icon (icons/chevron-left)
        :on-touch-tap #(on-page-change (- offset limit))}])
    (when (= items-count limit)
      [ui/flat-button
       {:secondary true
        :label next-button-text
        :label-position :before
        :icon (icons/chevron-right)
        :on-touch-tap #(on-page-change (+ offset limit))}])]])
