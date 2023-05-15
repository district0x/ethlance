(ns ethlance.ui.page.jobs.graphql
  (:require ))

(defn jobs-query [query-params]
  [:job-search query-params
   [:total-count
    [:items [:job/id
             :job/title
             :job/description
             :job/required-experience-level
             :job/bid-option
             :job/estimated-project-length
             :job/required-availability
             :job/date-created
             :job/required-skills
             [:job-stories [:total-count]]

             [:job/employer
              [:user/id
               :employer/rating
               [:user [:user/name
                      :user/country]]
               [:employer/feedback [:total-count]]]]

             [:job/arbiter
              [:user/id
               :arbiter/rating
               [:user [:user/name
                      :user/country]]
               [:arbiter/feedback [:total-count]]]]

             ]]]])

