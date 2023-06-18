(ns ethlance.ui.page.jobs.graphql)

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

             :job/token-type
             :job/token-amount
             [:token-details [:token-detail/id
                              :token-detail/name
                              :token-detail/symbol]]

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
               [:arbiter/feedback [:total-count]]]]]]]])

