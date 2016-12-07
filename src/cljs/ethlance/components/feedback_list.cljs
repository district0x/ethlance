(ns ethlance.components.feedback-list
  (:require
    [cljs-react-material-ui.icons :as icons]
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.list-pagination :refer [list-pagination]]
    [ethlance.components.message-bubble :refer [message-bubble]]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a]]
    [ethlance.components.star-rating :refer [star-rating]]
    [ethlance.components.truncated-text :refer [truncated-text]]
    [ethlance.constants :as constants]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [medley.core :as medley]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    ))

(defn feedback-star-rating [{:keys [rating style]}]
  [star-rating
   {:value (u/rating->star rating)
    :small? true
    :star-style style
    :display-number? true
    :rating-number-style style
    :style styles/feedback-style-rating}])

(defn feedback-list [{:keys [list-subscribe initial-dispatch]}]
  (let [list (subscribe list-subscribe)]
    (dispatch (into [:after-eth-contracts-loaded] initial-dispatch))
    (fn [{:keys [title pagination-props]}]
      (let [{:keys [loading? items offset limit]} @list]
        [paper
         {:loading? loading?
          :style styles/paper-section-main}
         [:h2 (or title "Feedback")]
         (for [item items]
           (let [{:keys [:contract/employer-feedback :contract/employer-feedback-rating
                         :contract/employer-feedback-on :contract/freelancer
                         :contract/freelancer-feedback :contract/freelancer-feedback-on
                         :contract/freelancer-feedback-rating
                         :contract/job :contract/done-by-freelancer? :contract/id]} item
                 {:keys [:job/employer]} job
                 employer-bubble (when employer-feedback-on
                                   [message-bubble
                                    {:side :right
                                     :user employer
                                     :date employer-feedback-on}
                                    [:div
                                     [feedback-star-rating
                                      {:rating freelancer-feedback-rating
                                       :style styles/white-text}]
                                     [truncated-text employer-feedback]]])
                 freelancer-bubble (when freelancer-feedback-on
                                     [message-bubble
                                      {:side :left
                                       :user freelancer
                                       :date freelancer-feedback-on}
                                      [:div
                                       [feedback-star-rating
                                        {:rating employer-feedback-rating
                                         :style styles/dark-text}]
                                       [truncated-text freelancer-feedback]]])]
             (if done-by-freelancer?
               [:div
                {:key id}
                freelancer-bubble
                employer-bubble]
               [:div
                {:key id}
                employer-bubble
                freelancer-bubble]))
           )]))))