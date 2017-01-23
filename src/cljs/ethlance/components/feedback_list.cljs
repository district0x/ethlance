(ns ethlance.components.feedback-list
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.list-pagination :refer [list-pagination]]
    [ethlance.components.message-bubble :refer [message-bubble]]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a]]
    [ethlance.components.show-more-pagination :refer [show-more-pagination]]
    [ethlance.components.star-rating :refer [star-rating]]
    [ethlance.constants :as constants]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [medley.core :as medley]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [ethlance.ethlance-db :as ethlance-db]))

(defn feedback-star-rating [{:keys [rating style]}]
  [star-rating
   {:value (u/rating->star rating)
    :small? true
    :star-style style
    :show-number? true
    :rating-number-style style
    :style styles/feedback-style-rating}])

(defn remove-without-feedback [items]
  (remove #(and (not (:contract/freelancer-feedback-on %))
                (not (:contract/employer-feedback-on %))) items))

(defn feedback-list [{:keys [:list-subscribe]}]
  (let [list (subscribe list-subscribe)]
    (fn [{:keys [:title :initial-dispatch :list-db-path :all-ids-subscribe]}]
      (let [{:keys [:loading? :items :offset :limit :show-more-limit :initial-limit :lines-in-message]
             :or {lines-in-message 5}} @list
            items (remove-without-feedback items)]
        [misc/call-on-change
         {:args initial-dispatch
          :on-change #(dispatch (conj [:after-eth-contracts-loaded] %))
          :load-on-mount? true}
         [paper
          {:loading? loading?}
          [:h2 {:style styles/margin-bottom-gutter} (or title "Feedback")]
          (if (seq items)
            (for [item items]
              (let [{:keys [:contract/employer-feedback :contract/employer-feedback-rating
                            :contract/employer-feedback-on :contract/freelancer
                            :contract/freelancer-feedback :contract/freelancer-feedback-on
                            :contract/freelancer-feedback-rating
                            :contract/job :contract/done-by-freelancer? :contract/id]} item
                    {:keys [:job/employer]} job
                    employer-bubble [message-bubble
                                     {:side :right
                                      :user employer
                                      :date employer-feedback-on
                                      :lines lines-in-message
                                      :header [feedback-star-rating
                                               {:rating employer-feedback-rating
                                                :style styles/white-text}]}
                                     (if employer-feedback-on
                                       employer-feedback
                                       [:div {:style styles/italic-text} "(Employer hasn't left feedback yet)"])]
                    freelancer-bubble [message-bubble
                                       {:side :left
                                        :user freelancer
                                        :date freelancer-feedback-on
                                        :lines lines-in-message
                                        :header [feedback-star-rating
                                                 {:rating freelancer-feedback-rating
                                                  :style styles/dark-text}]}
                                       (if freelancer-feedback-on
                                         freelancer-feedback
                                         [:div {:style styles/italic-text} "(Freelancer hasn't left feedback yet)"])]]
                [:div
                 {:key id}
                 (if done-by-freelancer?
                   [:div
                    freelancer-bubble
                    employer-bubble]
                   [:div
                    employer-bubble
                    freelancer-bubble])
                 (when-not (= item (last items))
                   [misc/hr])]))
            (when-not loading?
              [row-plain
               {:center "xs"}
               "No feedback left yet"]))
          [show-more-pagination
           {:all-ids-subscribe all-ids-subscribe
            :list-db-path list-db-path
            :load-dispatch [:contract.db/load-contracts ethlance-db/feedback-schema]
            :load-per 1
            :offset offset
            :initial-limit initial-limit
            :limit limit
            :loading? loading?
            :show-more-limit show-more-limit}]]]))))