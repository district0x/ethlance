(ns ethlance.pages.employer-detail-page
  (:require
    [cljs-react-material-ui.icons :as icons]
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.feedback-list :refer [feedback-list]]
    [ethlance.components.jobs-table :refer [jobs-table]]
    [ethlance.components.languages-chips :refer [languages-chips]]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a center-layout]]
    [ethlance.components.star-rating :refer [star-rating]]
    [ethlance.components.truncated-text :refer [truncated-text]]
    [ethlance.constants :as constants]
    [ethlance.ethlance-db :as ethlance-db]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [goog.string :as gstring]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn employer-info [{:keys [:user/gravatar :user/name :user/country :user/created-on :freelancer/description
                             :user/languages :user/id :user/status :employer/avg-rating :employer/total-paid
                             :employer/description :employer/ratings-count :user/address :user/balance] :as user}]
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
       [star-rating
        {:value (u/rating->star avg-rating)
         :show-number? true
         :ratings-count ratings-count}]
       [misc/country-marker
        {:country country}]]
      [col {:xs 6 :md 3
            :style styles/text-right}
       (when (= status 2)
         [row-plain
          {:end "xs"}
          [misc/blocked-user-chip]])
       [misc/elegant-line "spent" (u/eth total-paid)]
       [misc/elegant-line "balance" (u/eth balance)]]]
     [misc/hr]
     [misc/user-address address]
     [misc/user-created-on created-on]
     [truncated-text description]
     [misc/subheader "Speaks languages"]
     [misc/call-on-change
      {:load-on-mount? true
       :args {id (select-keys user [:user/languages-count])}
       :on-change #(dispatch [:after-eth-contracts-loaded [:contract.db/load-user-languages %]])}
      [languages-chips
       {:value languages}]]]))

(defn employer-detail [{:keys [:user/id :user/name :user/gravatar :user/employer? :user/freelancer?] :as user}]
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
        [:h2 "Employer Profile"]]
       [col
        {:xs 12 :md 6
         :style styles/text-right}
        (when freelancer?
          [ui/raised-button
           {:label "See freelancer profile"
            :primary true
            :href (u/path-for :freelancer/detail :user/id id)}])]]
      (if employer?
        [employer-info user]
        [row-plain
         {:center "xs"}
         "This user is not registered as a employer"])])])

(defn employer-jobs [{:keys [:user/id]}]
  [jobs-table
   {:list-subscribe [:list/jobs :list/employer-jobs]
    :show-status? true
    :initial-dispatch {:list-key :list/employer-jobs
                       :fn-key :ethlance-views/get-employer-jobs
                       :load-dispatch-key :contract.db/load-jobs
                       :schema (select-keys ethlance-db/job-schema
                                            [:job/title :job/total-paid :job/created-on :job/status])
                       :args {:user/id id :job/status 0}}
    :all-ids-subscribe [:list/ids :list/employer-jobs]
    :title "Employer's Jobs"
    :no-items-text "This employer didn't create any jobs"}])

(defn employer-feedback [{:keys [:user/id]}]
  [feedback-list
   {:list-subscribe [:list/contracts :list/employer-feedbacks]
    :initial-dispatch [:list/load-ids {:list-key :list/employer-feedbacks
                                       :fn-key :ethlance-views/get-employer-contracts
                                       :load-dispatch-key :contract.db/load-contracts
                                       :schema ethlance-db/feedback-schema
                                       :args {:user/id id :contract/status 4 :job/status 0}}]}])

(defn employer-detail-page []
  (let [user-id (subscribe [:user/route-user-id])
        user (subscribe [:user/detail])]
    (dispatch [:after-eth-contracts-loaded
               [:contract.db/load-users (merge ethlance-db/user-schema
                                               ethlance-db/employer-schema
                                               ethlance-db/user-balance-schema) [@user-id]]])
    (fn []
      [center-layout
       [employer-detail @user]
       (when (:user/employer? @user)
         [:div
          [employer-jobs @user]
          [employer-feedback @user]])])))