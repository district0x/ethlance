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
  {:user/name "Employerr"
   :user/gravatar "aaaabbb"
   :user/country 1
   :user/languages [1]
   :employer/description "hahaha"})

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

(def invitation1
  {:job-action/job 1
   :job-action/freelancer 1
   :invitation/description "Hello come here"})

(def proposal1
  {:job-action/job 1
   :proposal/rate 100
   :proposal/description "Hello I'm here"})

(def contract1
  {:contract/job 1
   :contract/rate 10
   :contract/hiring-done? true})

(def invoice1
  {:invoice/contract 1
   :invoice/description "Pay me"
   :invoice/amount 10
   :invoice/worked-hours 5
   :invoice/worked-from 10
   :invoice/worked-to 10})

(def invoice2
  {:invoice/contract 1
   :invoice/description "Pay me again"
   :invoice/amount 5
   :invoice/worked-hours 100
   :invoice/worked-from 1480407621
   :invoice/worked-to 1480407621})

(def feedback1
  {:contract/id 1
   :contract/feedback "good employer"
   :contract/rating 98})

(def feedback2
  {:contract/id 1
   :contract/feedback "good freelancer"
   :contract/rating 95})

(defn get-address [n]
  (nth (:my-addresses @re-frame.db/app-db) n))

(def skills1
  {:skill/names ["Clojurescript" "Ethereum" "Solidity"]})

(reg-event-fx
  :generate-db
  [trim-v]
  (fn [{:keys [db]}]
    {:dispatch-n [[:contract.user/register-freelancer freelancer1 (get-address 0)]]
     :dispatch-later [{:ms 10 :dispatch [:contract.user/register-employer employer1 (get-address 1)]}
                      {:ms 10 :dispatch [:contract.config/add-skills skills1 (get-address 0)]}
                      {:ms 20 :dispatch [:contract.job/add job1 (get-address 1)]}
                      {:ms 30 :dispatch [:contract.job/add-invitation invitation1 (get-address 1)]}
                      {:ms 40 :dispatch [:contract.job/add-proposal proposal1 (get-address 0)]}
                      {:ms 50 :dispatch [:contract.contract/add contract1 (get-address 1)]}
                      {:ms 60 :dispatch [:contract.invoice/add invoice1 (get-address 0)]}
                      {:ms 70 :dispatch [:contract.invoice/pay {:invoice/id 1} (:invoice/amount invoice1) (get-address 1)]}
                      {:ms 80 :dispatch [:contract.invoice/add invoice2 (get-address 0)]}
                      {:ms 90 :dispatch [:contract.invoice/cancel {:invoice/id 2} (get-address 0)]}
                      {:ms 100 :dispatch [:contract.contract/add-feedback feedback1 (get-address 0)]}
                      {:ms 110 :dispatch [:contract.contract/add-feedback feedback2 (get-address 1)]}
                      ]}))


