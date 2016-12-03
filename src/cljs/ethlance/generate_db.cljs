(ns ethlance.generate-db
  (:require [re-frame.core :refer [reg-event-db reg-event-fx trim-v after reg-fx console dispatch]]
            [ethlance.utils :as u]
            [cljs-web3.eth :as web3-eth]
            [clojure.data :as data]
            [clojure.string :as string]))

(defn rand-uint-coll [max-n max-int]
  (repeatedly (inc (rand-int max-n)) #(inc (rand-int max-int))))

(defn rand-text [max-chars]
  (let [c (inc (rand-int (/ max-chars (inc (rand-int 8)))))]
    (u/truncate (string/join " " (repeatedly c #(u/rand-str (inc (rand-int 12))))) max-chars "")))

(comment
  (rand-text 300))

(defn get-instance [key]
  (get-in @re-frame.db/app-db [:eth/contracts key :instance]))

(def freelancer1
  {:user/name "Matúš Lešťan"
   :user/gravatar "bfdb252fe9d0ab9759f41e3c26d7700e"
   :user/country 1
   :user/languages [1]
   :freelancer/available? true
   :freelancer/job-title "Clojure(script), Ethereum developer"
   :freelancer/hourly-rate 8
   :freelancer/categories [1 2]
   :freelancer/skills [1 2 3 4 5]
   :freelancer/description "My name is Matúš Lešťan asdasdasd" #_(doall (reduce str (range 100)))})

(def freelancer3
  #_(assoc freelancer1 :freelancer/skills #{4 6 9 25 28})
  (assoc freelancer1 :freelancer/skills #{10 11 12}))

(def freelancer4
  ;(assoc freelancer1 :freelancer/skills #{14 16 1 13 8 12 9 15}))
  (assoc freelancer1 :freelancer/skills #{1 2 3 4 5 6}))

(defn gen-freelancer []
  {:user/name (rand-text 40)
   :user/gravatar (u/rand-str 32)
   :user/country (rand-int 200)
   :user/languages (set (rand-uint-coll 6 200))
   :freelancer/available? true
   :freelancer/job-title (rand-text 40)
   :freelancer/hourly-rate (rand-int 100)
   :freelancer/categories (set (rand-uint-coll 6 6))
   :freelancer/skills (set (rand-uint-coll 10 29))
   :freelancer/description (rand-text 300)})

(defn gen-job []
  {:job/title (rand-text 90)
   :job/description (rand-text 300)
   :job/skills (set (rand-uint-coll 6 29))
   :job/budget (rand-int 100)
   :job/language (inc (rand-int 200))
   :job/category (inc (rand-int 7))
   :job/payment-type (inc (rand-int 2))
   :job/experience-level (inc (rand-int 3))
   :job/estimated-duration (inc (rand-int 3))
   :job/hours-per-week (inc (rand-int 2))
   :job/freelancers-needed (inc (rand-int 10))})

(def employer1
  {:user/name "SomeCorp."
   :user/gravatar "bfdb252fe9d0ab9759f41e3c26d7700f"
   :user/country 21
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
  {:skill/names (set (repeatedly 30 #(rand-text 20)))})

(reg-event-fx
  :generate-db
  [trim-v]
  (fn [{:keys [db]}]
    {:dispatch-n [[:contract.user/register-freelancer freelancer1 (get-address 0)]]
     :dispatch-later (concat
                       [{:ms 10 :dispatch [:contract.user/register-employer employer1 (get-address 1)]}]
                       [{:ms 10 :dispatch [:contract.config/add-skills skills1 (get-address 0)]}]
                       ;[{:ms 20 :dispatch [:contract.user/register-freelancer freelancer3 (get-address 0)]}]
                       ;[{:ms 30 :dispatch [:contract.user/register-freelancer freelancer4 (get-address 0)]}]
                       (map #(hash-map :ms 15 :dispatch [:contract.job/add (gen-job) (get-address 1)]) (range 2 10))
                       (map #(hash-map :ms 20 :dispatch [:contract.user/register-freelancer (gen-freelancer) (get-address %)]) (range 2 10))
                       [{:ms 30 :dispatch [:contract.job/add-invitation invitation1 (get-address 1)]}
                        {:ms 40 :dispatch [:contract.job/add-proposal proposal1 (get-address 0)]}
                        {:ms 50 :dispatch [:contract.contract/add contract1 (get-address 1)]}
                        {:ms 60 :dispatch [:contract.invoice/add invoice1 (get-address 0)]}
                        {:ms 70 :dispatch [:contract.invoice/pay {:invoice/id 1} (:invoice/amount invoice1) (get-address 1)]}
                        {:ms 80 :dispatch [:contract.invoice/add invoice2 (get-address 0)]}
                        {:ms 90 :dispatch [:contract.invoice/cancel {:invoice/id 2} (get-address 0)]}
                        {:ms 100 :dispatch [:contract.contract/add-feedback feedback1 (get-address 0)]}
                        {:ms 110 :dispatch [:contract.contract/add-feedback feedback2 (get-address 1)]}])

     }))


(comment
  (dispatch [:contract.job/add-invitation invitation1 (get-address 1)])

  (let [coll1 (set (rand-uint-coll 50 100))
        coll2 (set (rand-uint-coll 50 100))
        [added removed] (web3-eth/contract-call (get-instance :ethlance-user) :diff coll1 coll2)
        intersection (u/big-nums->nums (web3-eth/contract-call (get-instance :ethlance-user) :intersect coll1 coll2))
        added (u/big-nums->nums added)
        removed (u/big-nums->nums removed)
        correct-removed (sort (vec (clojure.set/difference coll1 coll2)))
        correct-added (sort (vec (clojure.set/difference coll2 coll1)))
        corrent-intersection (sort (vec (clojure.set/intersection coll1 coll2)))]
    (.clear js/console)
    ;(print.foo/look coll1)
    ;(print.foo/look coll2)
    ;(print.foo/look correct-removed)
    ;(print.foo/look correct-added)
    [(= added (vec correct-added)) (= removed correct-removed)
     (= (print.foo/look intersection) corrent-intersection)]))