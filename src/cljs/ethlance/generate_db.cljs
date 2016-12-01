(ns ethlance.generate-db
  (:require [re-frame.core :refer [reg-event-db reg-event-fx trim-v after reg-fx console dispatch]]))

(def freelancer1
  {:user/name "Matúš Lešťan"
   :user/gravatar "abc"
   :user/country 1
   :user/languages [1]
   :freelancer/available? true
   :freelancer/job-title "Cljs dev"
   :freelancer/hourly-rate 8
   :freelancer/categories [1 2]
   :freelancer/skills [3 4 5]
   :freelancer/description "My name is Matúš Lešťan asdasdasd" #_(doall (reduce str (range 100)))})

(def employer1
  {:user/name "Employerr"
   :user/gravatar "aaaabbb"
   :user/country 2
   :user/languages [1]
   :employer/description "hahaha"})

(def job1
  {:job/title "Ionic/AngularJs Mobile App Developer"
   :job/description "We are porting our jQueryMobile-based app to ionic, and have another ionic app, but have periodic development needs.\n\nPlease read these and respond in your application.\n\nYou MUST be available during US hours using Google Chat for video-conferencing and screen sharing.\n\nYou MUST be the person doing the work, not subcontracting it out to someone else.\n\nYou need to  use the upwork timer."
   :job/skills [2 3 4]
   :job/budget 10
   :job/language 1
   :job/category 1
   :job/payment-type 1
   :job/experience-level 1
   :job/estimated-duration 1
   :job/hours-per-week 1
   :job/freelancers-needed 2})

(def job2
  {:job/title "Bitcoin and Blockchain content writer for 800 - 1000 word article"
   :job/description "New Blockchain startup is looking for content writers who understand the blockchain, bitcoin and cryptocurrency space.\n\nWe are looking for a mix of Technical but made simple, but delivered in a way anyone can understand. Our audience are investors, and entrepreneurs \n\n\nPlease provide us with previous work \n\nTopics to write about\n\nBlockchain, crypto, how to,  ethereum, \n\netc..."
   :job/skills [5 6 7]
   :job/budget 10
   :job/language 2
   :job/category 3
   :job/payment-type 2
   :job/experience-level 2
   :job/estimated-duration 2
   :job/hours-per-week 2
   :job/freelancers-needed 1})

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
  {:skill/names ["Clojurescript" "Ethereum" "Solidity" "Bitcoin" "Blockchain" "English" "Ghostwriting" "Writing"]})

(reg-event-fx
  :generate-db
  [trim-v]
  (fn [{:keys [db]}]
    {:dispatch-n [[:contract.user/register-freelancer freelancer1 (get-address 0)]]
     :dispatch-later (concat
                       [{:ms 10 :dispatch [:contract.user/register-employer employer1 (get-address 1)]}]
                       [{:ms 10 :dispatch [:contract.config/add-skills skills1 (get-address 0)]}]
                       (map #(hash-map :ms 20 :dispatch [:contract.job/add % (get-address 1)]) (take 16 (cycle [job1 job2]))))
     ;{:ms 30 :dispatch [:contract.job/add-invitation invitation1 (get-address 1)]}
     ;{:ms 40 :dispatch [:contract.job/add-proposal proposal1 (get-address 0)]}
     ;{:ms 50 :dispatch [:contract.contract/add contract1 (get-address 1)]}
     ;{:ms 60 :dispatch [:contract.invoice/add invoice1 (get-address 0)]}
     ;{:ms 70 :dispatch [:contract.invoice/pay {:invoice/id 1} (:invoice/amount invoice1) (get-address 1)]}
     ;{:ms 80 :dispatch [:contract.invoice/add invoice2 (get-address 0)]}
     ;{:ms 90 :dispatch [:contract.invoice/cancel {:invoice/id 2} (get-address 0)]}
     ;{:ms 100 :dispatch [:contract.contract/add-feedback feedback1 (get-address 0)]}
     ;{:ms 110 :dispatch [:contract.contract/add-feedback feedback2 (get-address 1)]}
     }))


