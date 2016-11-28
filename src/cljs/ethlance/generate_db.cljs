(ns ethlance.generate-db
  (:require [re-frame.core :refer [reg-event-db reg-event-fx trim-v after reg-fx console dispatch]]))

(def freelancer1
  {:user/name "Mataaa"
   :user/gravatar "abc"
   :user/country 1
   :user/languages [1]
   :freelancer/available? true
   :freelancer/job-title "Cljs dev"
   :freelancer/hourly-rate 8
   :freelancer/categories [1 2]
   :freelancer/skills [3 4 5]
   :freelancer/description "asdasdasd" #_(doall (reduce str (range 100)))})

(def employer1
  {:employer/description "hahaha"})

(def job1
  {:job/title "This is Job 1"
   :job/description "Asdkaas  aspokd aps asopdk ap"
   :job/skills [2 3 4]
   :job/budget 10
   :job/language 1
   :job/category 1
   :job/payment-type 1
   :job/experience-level 1
   :job/estimated-duration 1
   :job/hours-per-week 1
   :job/freelancers-needed 2})

(reg-event-fx
  :generate-db
  [trim-v]
  (fn [{:keys [db]}]
    {:dispatch-n [[:contract.user/register-freelancer freelancer1]]
     :dispatch-later [{:ms 10 :dispatch [:contract.user/register-employer employer1 true]}
                      {:ms 50 :dispatch [:contract.job/add job1]}]}))


