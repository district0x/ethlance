(ns ethlance.pages.job-detail-page
  (:require
    [cljs-react-material-ui.icons :as icons]
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a]]
    [ethlance.components.skills-chips :refer [skills-chips]]
    [ethlance.components.star-rating :refer [star-rating]]
    [ethlance.constants :as constants]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))


(defn job-activity []
  (let [job (subscribe [:job/detail])]
    (fn []
      (let [{:keys [:job/freelancers-needed :job/contracts-count
                    :job/invitations-count :job/proposals-count]} @job]

        [:div
         [line "Proposals" proposals-count]
         [line "Invitations" invitations-count]
         [line "Freelancers Needed" freelancers-needed]
         [line "Hires" contracts-count]]))))

(defn employer-details []
  (let [job (subscribe [:job/detail])]
    (fn []
      (let [{:keys [:user/name :user/gravatar :user/id :employer/avg-rating
                    :employer/total-paid :employer/ratings-count]} (:job/employer @job)]

        [row
         {:middle "xs"}
         [col
          {:md 2}
          [ui/avatar
           {:size 80
            :src (u/gravatar-url gravatar)}]]
         [col
          [:h2 [a {:route :employer/detail
                   :route-params {:user/id id}} name]]
          [row-plain
           {:middle "xs"}
           [star-rating
            {:value (u/rating->star avg-rating)
             :star-style styles/star-rating-small}]
           [:h4 {:style {:margin-left 5
                         :color styles/primary1-color}} (u/round (u/rating->star avg-rating))]]
          [line (str (u/eth total-paid) " spent")]
          [line (str ratings-count " " (u/pluralize "feedback" ratings-count))]
          ]]))))

(defn job-details []
  (let [job (subscribe [:job/detail])
        active-page (subscribe [:db/active-page])]
    (dispatch [:contract/initiate-load :contract.db/load-jobs
               [(js/parseInt (get-in @active-page [:route-params :job/id]))]])
    (fn []
      (let [{:keys [:job/title :job/id :job/payment-type :job/estimated-duration
                    :job/experience-level :job/hours-per-week :job/created-on
                    :job/description :job/budget :job/skills :job/category
                    :job/status :job/hiring-done-on]} @job]
        [paper
         {:loading? (empty? title)}
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
            [job-activity]
            [misc/hr]
            [employer-details]
            ])]))))

(defn job-detail-page []
  [row
   {:center "xs"}
   [col
    {:lg 8
     :style styles/text-left}
    [job-details]]])
