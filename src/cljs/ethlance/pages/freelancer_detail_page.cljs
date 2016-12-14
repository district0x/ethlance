(ns ethlance.pages.freelancer-detail-page
  (:require
    [cljs-react-material-ui.icons :as icons]
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.categories-chips :refer [categories-chips]]
    [ethlance.components.contracts-table :refer [contracts-table]]
    [ethlance.components.feedback-list :refer [feedback-list]]
    [ethlance.components.languages-chips :refer [languages-chips]]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a center-layout]]
    [ethlance.components.skills-chips :refer [skills-chips]]
    [ethlance.components.star-rating :refer [star-rating]]
    [ethlance.components.truncated-text :refer [truncated-text]]
    [ethlance.ethlance-db :as ethlance-db]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [goog.string :as gstring]
    [re-frame.core :refer [subscribe dispatch]]
    [ethlance.constants :as constants]))

(defn freelancer-info [{:keys [:user/gravatar :user/name :user/country :freelancer/job-title :freelancer/avg-rating
                               :freelancer/ratings-count :freelancer/hourly-rate :freelancer/total-earned
                               :freelancer/available? :user/created-on :freelancer/description
                               :freelancer/skills :freelancer/categories :user/languages :user/id] :as user}]
  (when (seq name)
    [:div
     [row
      {:middle "xs"}
      [col
       {:xs 6 :md 2}
       [ui/avatar
        {:size 110
         :src (u/gravatar-url gravatar)}]]
      [col
       {:xs 6 :md 7}
       [:h1 name]
       [:h3 job-title]
       [star-rating
        {:value (u/rating->star avg-rating)
         :show-number? true
         :ratings-count ratings-count}]
       [misc/country-marker
        {:country country}]]
      [col {:xs 6 :md 3
            :style styles/text-right}
       [row-plain
        {:end "xs"}
        [misc/status-chip
         {:background-color (styles/freelancer-available?-color available?)}
         (if available?
           "available for hire!"
           "not available for hire")]]
       [misc/elegant-line "rate per hour" (u/eth hourly-rate)]
       [misc/elegant-line "earned" (u/eth total-earned)]]]
     [misc/hr]
     [:h4 {:style (merge styles/fade-text
                         {:margin-bottom 5})} "joined on " (u/format-date created-on)]
     [truncated-text description]
     [misc/subheader "Skills"]
     [skills-chips
      {:selected-skills skills
       :always-show-all? true}]
     [misc/subheader "Interested in categories"]
     [misc/call-on-change
      {:load-on-mount? true
       :args {id (select-keys user [:freelancer/categories-count])}
       :on-change #(dispatch [:after-eth-contracts-loaded [:contract.db/load-freelancer-categories %]])}
      [categories-chips
       {:value categories}]]
     [misc/subheader "Speaks languages"]
     [misc/call-on-change
      {:load-on-mount? true
       :args {id (select-keys user [:user/languages-count])}
       :on-change #(dispatch [:after-eth-contracts-loaded [:contract.db/load-user-languages %]])}
      [languages-chips
       {:value languages}]]]))

(defn freelancer-detail [{:keys [:user/id :user/name :user/gravatar :user/employer? :user/freelancer?] :as user}]
  [paper
   {:loading? (empty? name)
    :style styles/paper-section-main}
   (when (seq name)
     [:div
      [row
       {:middle "xs"
        :style styles/margin-bottom-gutter}
       [col
        {:xs 12 :md 6}
        [:h2 "Freelancer Profile"]]
       [col
        {:xs 12 :md 6
         :style styles/text-right}
        (when employer?
          [ui/raised-button
           {:label "See employer profile"
            :primary true
            :href (u/path-for :employer/detail :user/id id)}])]]
      (if freelancer?
        [freelancer-info user]
        [row-plain
         {:center "xs"}
         "This user is not registered as a freelancer"])])])

(defn freelancer-contracts [{:keys [:user/id]}]
  [contracts-table
   {:list-subscribe [:list/freelancer-contracts]
    :show-rate? true
    :show-total-paid? true
    :show-job? true
    :show-status? true
    :initial-dispatch {:list-key :list/freelancer-contracts
                       :fn-key :views/get-freelancer-contracts
                       :load-dispatch-key :contract.db/load-contracts
                       :schema (select-keys ethlance-db/contract-all-schema
                                            [:contract/job :contract/created-on :proposal/rate :contract/total-paid
                                             :contract/status])
                       :args {:user/id id :contract/status 0 :job/status 0}}
    :all-ids-subscribe [:list.ids/freelancer-contracts]
    :title "Job Activity"
    :no-items-text "Freelancer has no job activity"}])

(defn freelancer-feedback [{:keys [:user/id]}]
  [feedback-list
   {:list-subscribe [:list/freelancer-feedbacks]
    :initial-dispatch [:list/load-ids {:list-key :list/freelancer-feedbacks
                                       :fn-key :views/get-freelancer-contracts
                                       :load-dispatch-key :contract.db/load-contracts
                                       :schema ethlance-db/feedback-schema
                                       :args {:user/id id :contract/status 4 :job/status 0}}]}])

(defn freelancer-detail-page []
  (let [user-id (subscribe [:user/route-user-id])
        user (subscribe [:user/detail])]
    (dispatch [:after-eth-contracts-loaded
               [:contract.db/load-users (merge ethlance-db/user-schema ethlance-db/freelancer-schema) [@user-id]]])
    (fn []
      [center-layout
       [freelancer-detail @user]
       (when (:user/freelancer? @user)
         [:div
          [freelancer-contracts @user]
          [freelancer-feedback @user]])])))