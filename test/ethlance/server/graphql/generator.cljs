(ns ethlance.server.graphql.generator
  (:require

   [district.server.db :as db]
   [ethlance.server.db :as ethlance-db]
   [district.shared.async-helpers :refer [promise->]]
   [taoensso.timbre :as log]

   [clojure.string :as string]
   [cljs-time.core :as time]
   [cljs-time.coerce :as time-coerce]

   ))

(def lorem "Lorem ipsum dolor sit amet, consectetur adipiscing elit. In blandit auctor neque ut pharetra. Vivamus mollis ligula at ultrices cursus. Sed suscipit hendrerit nulla. Maecenas eleifend facilisis enim, eget imperdiet ipsum vestibulum id. Maecenas at dui ut purus tempor porttitor vitae vel mauris. In accumsan mattis est, eget sollicitudin nibh bibendum nec. Mauris posuere nisi pulvinar nibh dapibus varius. Nunc elementum arcu eu ex ullamcorper mattis. Proin porttitor viverra nisi, eu venenatis magna feugiat ultrices. Vestibulum justo justo, ullamcorper sit amet ultrices in, tempor non turpis.")

(def job-categories
  {0 "All Categories"
   1 "Web, Mobile & Software Dev"
   2 "IT & Networking"
   3 "Data Science & Analytics"
   4 "Design & Creative"
   5 "Writing"
   6 "Translation"
   7 "Legal"
   8 "Admin Support"
   9 "Customer Service"
   10 "Sales & Marketing"
   11 "Accounting & Consulting"
   12 "Other"})

(def languages ["en" "nl" "pl" "de" "es" "fr"])

(defn generate-user-languages [user-addresses]
  (js/Promise.
   (fn [resolve reject]
     (try
       (doall (for [address user-addresses language languages]
                (let [[speaks? _] (shuffle [true false])]
                  (when speaks?
                    (ethlance-db/insert-row! :UserLanguage {:user/address address
                                                            :language/id language})))))
       (resolve true)
       (catch :default e
         (log/error "Error" {:error e})
         (reject e))))))

(defn generate-categories [categories [_ candidate arbiter]]
  (js/Promise.
   (fn [resolve reject]
     (try
       (doall (for [category categories]
                (do
                  (ethlance-db/insert-row! :Category {:category/id category})

                  (ethlance-db/insert-row! :CandidateCategory {:user/address candidate
                                                               :category/id category})

                  (ethlance-db/insert-row! :ArbiterCategory {:user/address arbiter
                                                             :category/id category}))))
       (resolve true)
       (catch :default e
         (log/error "Error" {:error e})
         (reject e))))))

(defn generate-skills [skills [_ candidate arbiter]]
  (js/Promise.
   (fn [resolve reject]
     (try
       (doall (for [skill skills]
                (do
                  (ethlance-db/insert-row! :Skill {:skill/id skill})

                  (ethlance-db/insert-row! :CandidateSkill {:user/address candidate
                                                            :skill/id skill})

                  (ethlance-db/insert-row! :ArbiterSkill {:user/address arbiter
                                                          :skill/id skill}))))
       (resolve true)
       (catch :default e
         (log/error "Error" {:error e})
         (reject e))))))

(defn generate-users [user-addresses]
  (js/Promise.
   (fn [resolve reject]
     (try
       (doall (for [address user-addresses]
                (let [[country-code _] (shuffle ["US" "BE" "UA" "CA" "SLO" "PL"])
                      [first-name _] (shuffle ["Filip" "Juan" "Ben" "Matus"])
                      [second-name _] (shuffle ["Fu" "Bar" "Smith" "Doe" "Hoe"])
                      [extension _] (shuffle ["io" "com" "gov"])
                      [profile-id _] (shuffle (range 0 10))
                      [currency _] (shuffle ["EUR" "USD"])
                      date-registered (time-coerce/to-long (time/minus (time/now) (time/days (rand-int 60))))
                      from (rand-int 100)
                      bio (subs lorem from (+ 100 from))
                      [professional-title _] (shuffle ["Dr" "Md" "PhD" "Mgr" "Master of Wine and Whisky"])]
                  (ethlance-db/insert-row! :User {:user/address address
                                                  :user/country-code country-code
                                                  :user/user-name (str "@" first-name)
                                                  :user/full-name (str first-name " " second-name)
                                                  :user/email (string/lower-case (str first-name "@" second-name "." extension))
                                                  :user/profile-image (str "https://randomuser.me/api/portraits/lego/" profile-id ".jpg")
                                                  :user/date-registered date-registered
                                                  :user/date-updated date-registered})

                  (when (= "EMPLOYER" address)
                    (ethlance-db/insert-row! :Employer {:user/address address
                                                        :employer/bio bio
                                                        :employer/professional-title professional-title}))

                  (when (= "CANDIDATE" address)
                    (ethlance-db/insert-row! :Candidate {:user/address address
                                                         :candidate/rate (rand-int 200)
                                                         :candidate/rate-currency-id currency
                                                         :candidate/bio bio
                                                         :candidate/professional-title professional-title}))
                  (when (= "ARBITER" address)
                    (ethlance-db/insert-row! :Arbiter {:user/address address
                                                       :arbiter/bio bio
                                                       :arbiter/professional-title professional-title
                                                       :arbiter/fee (rand-int 200)
                                                       :arbiter/fee-currency-id currency})))))
       (resolve true)
       (catch :default e
         (log/error "Error" {:error e})
         (reject e))))))

(defn generate-jobs [job-ids [employer & _]]
  (js/Promise.
   (fn [resolve reject]
     (try
       (doall (for [job-id job-ids]
                (let [title (str (-> ["marmot" "deer" "mammut" "tiger" "lion" "elephant" "bobcat"] shuffle first) " "
                                 (-> ["control" "design" "programming" "aministartion" "development"] shuffle first))
                      from (rand-int 100)
                      description (subs lorem from (+ 20 from))
                      category (get job-categories (rand-int 13))
                      [status _] (shuffle ["Hiring" "Hiring Done"])
                      date-created (time/minus (time/now) (time/days (rand-int 60)))
                      date-published (time/plus date-created (time/days (rand-int 5)))
                      date-updated (time/plus date-published (time/days (rand-int 7)))
                      estimated-length (case (-> [:hours :days :weeks] shuffle first)
                                         :hours (time/hours (rand-int 24))
                                         :days (time/days (rand-int 30))
                                         :weeks (time/weeks (rand-int 100)))
                      [availability _] (shuffle ["Part Time" "Full Time"])
                      [expertise-level _] (shuffle ["Beginner" "Intermediate" "Expert"])
                      [bid-option _] (shuffle ["Hourly Rate" "Bounty"])
                      number-of-candidates (rand-int 5)
                      [invitation-only? _] (shuffle [true false])
                      token "0x8f389F672Ef0586628689f9227e1d0e09f9A3245"
                      reward (rand-int 300)
                      date-deadline (time/plus date-updated estimated-length)
                      [platform _] (shuffle ["mobile" "web" "embedded"])
                      [language _] (shuffle languages)]
                  (ethlance-db/insert-row! :Job {:job/id job-id
                                                 :job/bounty-id job-id
                                                 :job/title title
                                                 :job/description description
                                                 :job/category category
                                                 :job/status status
                                                 :job/date-created (time-coerce/to-long date-created)
                                                 :job/date-published (time-coerce/to-long date-published)
                                                 :job/date-updated (time-coerce/to-long date-updated)
                                                 :job/estimated-length (time/in-millis estimated-length)
                                                 :job/required-availability availability
                                                 :job/bid-option bid-option
                                                 :job/expertise-level expertise-level
                                                 :job/number-of-candidates number-of-candidates
                                                 :job/invitation-only? invitation-only?
                                                 :job/token token
                                                 :job/token-version 1
                                                 :job/reward reward
                                                 :job/date-deadline (time-coerce/to-long date-deadline)
                                                 :job/platform platform
                                                 :job/web-reference-url "http://this/that.com"
                                                 :job/language-id language})

                  (ethlance-db/insert-row! :JobCreator {:job/id job-id
                                                        :user/address employer}))))
       (resolve true)
       (catch :default e
         (log/error "Error" {:error e})
         (reject e))))))

(defn generate-job-arbiters [job-ids [employer candidate arbiter]]
  (js/Promise.
   (fn [resolve reject]
     (try
       (doall (for [job-id job-ids]
                (let [[status _] (shuffle ["invited" "accepted" ])
                      fee (rand-int 200)
                      [fee-currency-id _] (shuffle ["EUR" "USD"])]
                  (ethlance-db/insert-row! :JobArbiter {:job/id job-id
                                                        :user/address arbiter
                                                        :job-arbiter/fee fee
                                                        :job-arbiter/fee-currency-id fee-currency-id
                                                        :job-arbiter/status status}))))
       (resolve true)
       (catch :default e
         (log/error "Error" {:error e})
         (reject e))))))

(defn generate-message [{:keys [:message/creator :message/id]}]
  {:message/id id
   :message/creator creator
   :message/text (let [from (rand-int 200)]
                   (subs lorem from (+ 20 from)))
   :message/date-created (time-coerce/to-long (time/minus (time/now)
                                                          (time/days (rand-int 60))))})

(defn generate-contracts [contract-ids job-ids [employer candidate arbiter]]
  (js/Promise.
   (fn [resolve reject]
     (try
       (doall (for [contract-id contract-ids]
                (let [[job-id _] (shuffle job-ids)
                      [status _] (shuffle ["Proposal Pending" "Active" "Finished" "Cancelled"])
                      date-created (time/minus (time/now) (time/days (rand-int 60)))
                      last-message-index  (:count (db/get {:select [[:%count.* :count]]
                                                           :from [:Message]}))
                      invitation-message (generate-message {:message/creator employer
                                                            :message/id (+ last-message-index 1)})
                      proposal-message (generate-message {:message/creator candidate
                                                          :message/id (+ last-message-index 2)})
                      proposal-rate (rand-int 300)
                      [currency _] (shuffle ["EUR" "USD"])]

                  (ethlance-db/insert-row! :Message (merge invitation-message
                                                           {:message/type "INVITATION"}))
                  (ethlance-db/insert-row! :Message (merge proposal-message
                                                           {:message/type "PROPOSAL"}))

                  (ethlance-db/insert-row! :Contract {:contract/id contract-id
                                                      :job/id job-id
                                                      :contract/status status
                                                      :contract/date-created (time-coerce/to-long date-created)
                                                      :contract/date-updated (time-coerce/to-long date-created)
                                                      :contract/invitation-message-id (:message/id invitation-message)
                                                      :contract/proposal-message-id (:message/id proposal-message)
                                                      :contract/proposal-rate proposal-rate
                                                      :contract/proposal-rate-currency-id currency})

                  (ethlance-db/insert-row! :ContractCandidate {:contract/id contract-id
                                                               :user/address candidate}))))
       (resolve true)
       (catch :default e
         (log/error "Error" {:error e})
         (reject e))))))

(defn generate-disputes [contract-ids job-ids [employer candidate arbiter]]
  (js/Promise.
   (fn [resolve reject]
     (try
       (doall (for [job-id job-ids
                    contract-id contract-ids]
                (when (-> [true false] shuffle first)
                  (let [last-message-index (:count (db/get {:select [[:%count.* :count]]
                                                            :from [:Message]}))
                        raised-dispute-message (generate-message {:message/creator employer
                                                                  :message/id (+ last-message-index 1)})
                        resolved-dispute-message (generate-message {:message/creator candidate
                                                                    :message/id (+ last-message-index 2)})]
                    (ethlance-db/insert-row! :Message (merge raised-dispute-message
                                                             {:message/type "RAISED-DISPUTE"}))
                    (ethlance-db/insert-row! :Message (merge resolved-dispute-message
                                                             {:message/type "RESOLVED-DISPUTE"}))
                    (ethlance-db/insert-row! :Dispute {:job/id job-id
                                                       :contract/id contract-id
                                                       :dispute/raised-message-id (:message/id raised-dispute-message)
                                                       :dispute/resolved-message-id (:message/id resolved-dispute-message)})))))
       (resolve true)
       (catch :default e
         (log/error "Error" {:error e})
         (reject e))))))

(defn generate-invoices [invoice-ids contract-ids [employer candidate arbiter]]
  (js/Promise.
   (fn [resolve reject]
     (try
       (doall (for [invoice-id invoice-ids]
                (let [[contract-id _] (shuffle contract-ids)
                      last-message-index  (:count (db/get {:select [[:%count.* :count]]
                                                           :from [:Message]}))
                      invoice-message (generate-message {:message/creator candidate
                                                         :message/id (+ last-message-index 1)})
                      [status _] (shuffle ["PAID" "PENDING"])
                      date-work-started (time/minus (time/now) (time/days (rand-int 60)))
                      work-duration (case (-> [:hours :days :weeks] shuffle first)
                                      :hours (time/hours (rand-int 24))
                                      :days (time/days (rand-int 30))
                                      :weeks (time/weeks (rand-int 100)))
                      date-work-ended (time/plus date-work-started work-duration)
                      date-paid (when (= "PAID" status) (time-coerce/to-long (time/plus date-work-ended (time/days (rand-int 7)))))]
                  (ethlance-db/insert-row! :Message (merge invoice-message
                                                           {:message/type "INVOICE"}))
                  (ethlance-db/insert-row! :Invoice {:invoice/id invoice-id
                                                     :contract/id contract-id
                                                     :message/id (:message/id invoice-message)
                                                     :invoice/status status
                                                     :invoice/amount-requested (rand-int 12000)
                                                     :invoice/amount-paid (rand-int 12000)
                                                     :invoice/date-work-started (time-coerce/to-long date-work-started)
                                                     :invoice/work-duration (time/in-millis work-duration)
                                                     :invoice/date-work-ended (time-coerce/to-long date-work-ended)
                                                     :invoice/date-paid date-paid}))))
       (resolve true)
       (catch :default e
         (log/error "Error" {:error e})
         (reject e))))))

(defn generate-feedback [contract-ids [employer candidate arbiter]]
  (js/Promise.
   (fn [resolve reject]
     (try
       (doall (for [contract-id contract-ids]
                (let [last-message-index (:count (db/get {:select [[:%count.* :count]]
                                                          :from [:Message]}))

                      candidate-feedback-message-id (inc last-message-index)
                      candidate-feedback (generate-message {:message/creator employer
                                                            :message/id candidate-feedback-message-id})
                      candidate-rating (rand-int 5)

                      employer-feedback-message-id (+ last-message-index 2)
                      employer-feedback (generate-message {:message/creator candidate
                                                           :message/id employer-feedback-message-id})
                      employer-rating (rand-int 5)

                      arbiter-feedback-from-candidate-message-id (+ last-message-index 3)
                      arbiter-feedback-from-candidate (generate-message {:message/creator candidate
                                                                         :message/id arbiter-feedback-from-candidate-message-id})
                      arbiter-from-candidate-rating (rand-int 5)

                      arbiter-feedback-from-employer-message-id (+ last-message-index 4)
                      arbiter-feedback-from-employer (generate-message {:message/creator employer
                                                                        :message/id arbiter-feedback-from-employer-message-id})
                      arbiter-from-employer-rating (rand-int 5)                      ]
                  ;; feedback for the candidate
                  (ethlance-db/insert-row! :Message (merge candidate-feedback
                                                           {:message/type "CANDIDATE FEEDBACK"}))

                  (ethlance-db/insert-row! :Feedback {:contract/id contract-id
                                                      :message/id candidate-feedback-message-id
                                                      :feedback/rating candidate-rating})
                  ;; feedback for the employer
                  (ethlance-db/insert-row! :Message (merge employer-feedback
                                                           {:message/type "EMPLOYER FEEDBACK"}))

                  (ethlance-db/insert-row! :Feedback {:contract/id contract-id
                                                      :message/id employer-feedback-message-id
                                                      :feedback/rating employer-rating})

                  ;; feedback for the arbiter from candidate
                  (ethlance-db/insert-row! :Message (merge arbiter-feedback-from-candidate
                                                           {:message/type "ARBITER FEEDBACK"}))

                  (ethlance-db/insert-row! :Feedback {:contract/id contract-id
                                                      :message/id arbiter-feedback-from-candidate-message-id
                                                      :feedback/rating arbiter-from-candidate-rating})

                  ;; feedback for the arbiter from employer
                  (ethlance-db/insert-row! :Message (merge arbiter-feedback-from-employer
                                                           {:message/type "ARBITER FEEDBACK"}))

                  (ethlance-db/insert-row! :Feedback {:contract/id contract-id
                                                      :message/id arbiter-feedback-from-employer-message-id
                                                      :feedback/rating arbiter-from-employer-rating}))))
       (resolve true)
       (catch :default e
         (log/error "Error" {:error e})
         (reject e))))))

(defn generate-dev-data []
  (let [user-addresses ["EMPLOYER" "CANDIDATE" "ARBITER"]
        categories ["Web" "Mobile" "Embedded"]
        skills ["Solidity" "Clojure"]
        job-ids (map str (range 0 3))
        contract-ids (map str (range 0 5))
        invoice-ids (map str (range 0 10))]
    (promise->
     (generate-users user-addresses)
     #(generate-categories categories user-addresses)
     #(generate-skills skills user-addresses)
     #(generate-user-languages user-addresses)
     #(generate-jobs job-ids user-addresses)
     #(generate-job-arbiters job-ids user-addresses)
     #(generate-contracts contract-ids job-ids user-addresses)
     #(generate-disputes contract-ids job-ids user-addresses)
     #(generate-invoices invoice-ids contract-ids user-addresses)
     #(generate-feedback contract-ids user-addresses)
     #(log/debug "Done"))))
