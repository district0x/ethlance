(ns ethlance.pages.employer-detail-page
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [clojure.set :as set]
    [ethlance.components.feedback-list :refer [feedback-list]]
    [ethlance.components.jobs-table :refer [jobs-table]]
    [ethlance.components.languages-chips :refer [languages-chips]]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a center-layout currency]]
    [ethlance.components.star-rating :refer [star-rating]]
    [ethlance.components.user-detail :as user-detail]
    [ethlance.constants :as constants]
    [ethlance.ethlance-db :as ethlance-db]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [goog.string :as gstring]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn employer-info []
  (let [xs-width? (subscribe [:window/xs-width?])]
    (fn [{:keys [:user/gravatar :user/name :user/country :user/state :user/created-on :freelancer/description
                 :user/languages :user/id :user/status :employer/avg-rating :employer/total-paid
                 :employer/description :employer/ratings-count :user/address :user/balance] :as user}]
      (when (seq name)
        [:div
         [user-detail/user-info user (merge
                                       {:avg-rating avg-rating
                                        :ratings-count ratings-count
                                        :description description}
                                       (select-keys user [:employer/total-paid]))]
         [user-detail/languages-section user languages]]))))

(defn employer-detail []
  (let [xs-width? (subscribe [:window/xs-width?])]
    (fn [{:keys [:user/id :user/name :user/gravatar :user/employer? :user/freelancer?] :as user}]
      [paper
       {:loading? (empty? name)
        :style styles/paper-section-main}
       (when (seq name)
         [:div
          [row
           {:middle "xs"
            :start "sm"
            :center "xs"
            :style styles/margin-bottom-gutter}
           [col
            {:xs 12 :sm 6}
            [:h2 "Employer Profile"]]
           [col
            {:xs 12 :sm 6
             :style (if @xs-width?
                      {:margin-top 10}
                      styles/text-right)}
            (when freelancer?
              [ui/flat-button
               {:label "freelancer profile"
                :primary true
                :href (u/path-for :freelancer/detail :user/id id)}])]]
          (if employer?
            [employer-info user]
            [row-plain
             {:center "xs"}
             "This user is not registered as a employer"])])])))

(defn employer-jobs []
  (let [xs-width? (subscribe [:window/xs-width?])]
    (fn [{:keys [:user/id]}]
      [jobs-table
       {:list-subscribe [:list/jobs :list/employer-jobs]
        :show-status? true
        :show-created-on? (not @xs-width?)
        :show-total-paid? (not @xs-width?)
        :initial-dispatch {:list-key :list/employer-jobs
                           :fn-key :ethlance-views/get-employer-jobs
                           :load-dispatch-key :contract.db/load-jobs
                           :fields #{:job/title :job/total-paid :job/created-on :job/status}
                           :args {:user/id id :job/status 0}}
        :all-ids-subscribe [:list/ids :list/employer-jobs]
        :title "Employer's Jobs"
        :no-items-text "This employer didn't create any jobs"}])))

(defn employer-feedback [{:keys [:user/id]}]
  [feedback-list
   {:list-subscribe [:list/contracts :list/employer-feedbacks]
    :list-db-path [:list/employer-feedbacks]
    :all-ids-subscribe [:list/ids :list/employer-feedbacks]
    :initial-dispatch [:list/load-ids {:list-key :list/employer-feedbacks
                                       :fn-key :ethlance-views/get-employer-contracts
                                       :load-dispatch-key :contract.db/load-contracts
                                       :fields ethlance-db/feedback-entity-fields
                                       :args {:user/id id :contract/status 4 :job/status 0}}]}])

(defn employer-detail-page []
  (let [user-id (subscribe [:user/route-user-id])
        user (subscribe [:user/detail])]
    (fn []
      [misc/call-on-change
       {:load-on-mount? true
        :args @user-id
        :on-change #(dispatch [:after-eth-contracts-loaded
                               [:contract.db/load-users (set/union ethlance-db/user-entity-fields
                                                                   ethlance-db/employer-entity-fields
                                                                   ethlance-db/user-balance-entity-fields) [@user-id]]])}
       [center-layout
        [employer-detail @user]
        (when (:user/employer? @user)
          [:div
           [employer-jobs @user]
           [employer-feedback @user]])]])))