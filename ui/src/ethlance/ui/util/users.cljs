(ns ethlance.ui.util.users)


(defn job->participants
  [job-story]
  {:candidate (or
                (get-in job-story [:job-story/candidate])
                (get-in job-story [:candidate :user/id]))
   :employer (get-in job-story [:job :job/employer :user/id])
   :arbiter (get-in job-story [:job :job/arbiter :user/id])})


(defn user-type
  [active-user involved-users]
  (reduce (fn [acc [user-type address]]
            (if (and (nil? acc) (ilike= address active-user)) user-type acc))
          nil ; initial value
          involved-users))
