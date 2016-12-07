(ns ethlance.pages.job-detail-page
  (:require
    [cljs-react-material-ui.icons :as icons]
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.invoices-table :refer [invoices-table]]
    [ethlance.components.list-pagination :refer [list-pagination]]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a]]
    [ethlance.components.skills-chips :refer [skills-chips]]
    [ethlance.components.star-rating :refer [star-rating]]
    [ethlance.constants :as constants]
    [ethlance.ethlance-db :as ethlance-db]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    ))


(defn employer-details []
  (let [job (subscribe [:job/detail])]
    (fn []
      (let [{:keys [:user/name :user/gravatar :user/id :employer/avg-rating
                    :employer/total-paid :employer/ratings-count :user/country]} (:job/employer @job)
            route-props {:route :employer/detail
                         :route-params {:user/id id}}]
        [row
         {:middle "xs"}
         [col
          {:md 2}
          [a route-props
           [ui/avatar
            {:size 100
             :src (u/gravatar-url gravatar)}]]]
         [col
          [:h2 [a route-props name]]
          [row-plain
           {:middle "xs"}
           [star-rating
            {:value (u/rating->star avg-rating)
             :star-style styles/star-rating-small}]
           (when (pos? avg-rating)
             [:h4 {:style {:margin-left 5
                           :color styles/primary1-color}} (u/round (u/rating->star avg-rating))])]
          [line (str (u/eth total-paid) " spent")]
          [line (str ratings-count " " (u/pluralize "feedback" ratings-count))]
          [misc/country-marker
           {:country country}]]]))))

(defn job-contracts []
  (let [list (subscribe [:list/job-contracts])
        job-id (subscribe [:job/route-job-id])
        job (subscribe [:job/detail])
        my-job? (subscribe [:job/my-job?])
        active-user-id (subscribe [:db/active-user-id])]
    (dispatch [:contract/initiate-load :contract.views/load-job-contracts {:job/id @job-id}])
    (fn []
      (let [{:keys [loading? items offset limit]} @list
            {:keys [:job/payment-type]} @job]
        [paper
         {:loading? loading?
          :style styles/paper-section-main}
         [:h2 "Job Proposals"]
         [ui/table
          [ui/table-header
           [ui/table-row
            [ui/table-header-column "Freelancer"]
            [ui/table-header-column "Bid"]
            [ui/table-header-column "Time"]
            [ui/table-header-column "Status"]]]
          [ui/table-body
           {:show-row-hover @my-job?}
           (if (seq items)
             (for [item items]
               (let [{:keys [:contract/freelancer :contract/id :contract/status :proposal/rate]} item
                     my-contract? (= @active-user-id (:user/id freelancer))]
                 [ui/table-row
                  {:key id
                   :style (when (or @my-job? my-contract?) styles/clickable)
                   :on-touch-tap (when (or @my-job? my-contract?)
                                   (u/table-row-nav-to-fn :contract/detail {:contract/id id}))}
                  [ui/table-row-column
                   [a
                    {:route-params {:user/id (:user/id freelancer)}
                     :route :freelancer/detail}
                    (:user/name freelancer)]]
                  [ui/table-row-column
                   (if (= status 1) "-" (u/format-rate rate payment-type))]
                  [ui/table-row-column
                   (if (= status 1)
                     (u/time-ago (:invitation/created-on item))
                     (u/time-ago (:proposal/created-on item)))]
                  [ui/table-row-column
                   [misc/status-chip
                    {:background-color (styles/contract-status-colors status)}
                    (constants/contract-statuses status)]]]))
             (misc/create-no-items-row "There are no proposals for this job yet" loading?))]
          (misc/create-table-pagination
            {:all-subscribe [:list.ids/job-contracts]
             :list-db-path [:list/job-contracts]
             :load-dispatch [:contract.db/load-contracts
                             (ethlance-db/without-strings ethlance-db/proposal+invitation-schema)]
             :offset offset
             :limit limit})]]))))

(defn job-details []
  (let [job (subscribe [:job/detail])
        job-id (subscribe [:job/route-job-id])]
    (dispatch [:contract/initiate-load :contract.db/load-jobs ethlance-db/job-schema [@job-id]])
    (fn []
      (let [{:keys [:job/title :job/id :job/payment-type :job/estimated-duration
                    :job/experience-level :job/hours-per-week :job/created-on
                    :job/description :job/budget :job/skills :job/category
                    :job/status :job/hiring-done-on :job/freelancers-needed]} @job]
        [paper
         {:loading? (empty? title)
          :style styles/paper-section-main}
         (when id
           [:div
            [:h1 title]
            [:h3 {:style {:margin-top 5}} (constants/categories category)]
            [:h4 {:style styles/fade-text} "Posted on " (u/format-date created-on)]
            (when hiring-done-on
              [:h4 {:style styles/fade-text} "Hiring done on " (u/format-date hiring-done-on)])
            [row-plain
             {:style {:margin-top 20}}
             [misc/status-chip
              {:background-color (styles/job-status-colors status)}
              (constants/job-statuses status)]
             [misc/status-chip
              {:background-color (styles/job-payment-type-colors payment-type)}
              (constants/payment-types payment-type) " Rate"]
             [misc/status-chip
              {:background-color (styles/job-estimation-duration-colors estimated-duration)}
              "For " (constants/estimated-durations estimated-duration)]
             [misc/status-chip
              {:background-color (styles/job-experience-level-colors experience-level)}
              "For " (constants/experience-levels experience-level)]
             [misc/status-chip
              {:background-color (styles/job-hours-per-week-colors hours-per-week)}
              (constants/hours-per-weeks hours-per-week)]
             (when (u/big-num-pos? budget)
               [misc/status-chip
                {:background-color styles/budget-chip-color}
                "Budget " (u/eth budget)])]
            [:p {:style styles/detail-description} description]
            [u/subheader "Required Skills"]
            [skills-chips
             {:selected-skills skills
              :always-show-all? true}]
            [misc/hr]
            [employer-details]
            ])]))))

(defn job-invoices []
  (let [job-id (subscribe [:job/route-job-id])]
    (fn []
      [invoices-table
       {:list-subscribe [:list/job-invoices]
        :initial-dispatch [:contract.views/load-job-invoices {:job/id @job-id :invoice/status 0}]
        :show-freelancer? true
        :pagination-props {:all-subscribe [:list.ids/job-invoices]
                           :list-db-path [:list/job-invoices]
                           :load-dispatch [:contract.db/load-invoices ethlance-db/invoices-table-schema]}}])))

(defn job-detail-page []
  [misc/center-layout
   [job-details]
   [job-contracts]
   [job-invoices]])
