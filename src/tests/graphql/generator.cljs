(ns tests.graphql.generator
  (:require [cljs-time.coerce :as time-coerce]
            [cljs-time.core :as time]
            [clojure.string :as string]
            [district.shared.async-helpers :refer [safe-go <?]]
            [district.server.async-db :as async-db]
            [cljs.core.async :refer [go <!]]
            [ethlance.server.db :as ethlance-db]
            [taoensso.timbre :as log]
            [ethlance.shared.constants :as constants]))

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

(def languages (map (zipmap constants/languages constants/languages) ["English" "español"]))

(defn generate-user-languages [conn user-addresses]
  (safe-go
   (doseq [address user-addresses language languages]
     (let [[speaks? _] (shuffle [true false])]
       (when speaks?
         (<? (ethlance-db/insert-row! conn :UserLanguage {:user/address address
                                                          :language/id language})))))))

(defn generate-categories [conn categories [_ candidate arbiter]]
  (safe-go
   (doseq [category categories]
     (<? (ethlance-db/insert-row! conn :Category {:category/id category}))

     (<? (ethlance-db/insert-row! conn :CandidateCategory {:user/address candidate
                                                           :category/id category}))

     (<? (ethlance-db/insert-row! conn :ArbiterCategory {:user/address arbiter
                                                         :category/id category})))))

(defn generate-skills [conn skills [_ candidate arbiter]]
  (safe-go
   (doseq [skill skills]
     (<? (ethlance-db/insert-row! conn :Skill {:skill/id skill}))

     (<? (ethlance-db/insert-row! conn :CandidateSkill {:user/address candidate
                                                        :skill/id skill}))

     (<? (ethlance-db/insert-row! conn :ArbiterSkill {:user/address arbiter
                                                      :skill/id skill})))))

(defn generate-users [conn user-addresses]
  (safe-go
   (doseq [[address-owner address] user-addresses]
     (let [[country-code _] (shuffle ["United States" "Belgium" "United Arab Emirates" "Canada" "Slovakia" "Poland"])
           [first-name _] (shuffle ["Filip" "Juan" "Ben" "Matus"])
           [second-name _] (shuffle ["Fu" "Bar" "Smith" "Doe" "Hoe"])
           [extension _] (shuffle ["io" "com" "gov"])
           [profile-id _] (shuffle (range 0 10))
           [currency _] (shuffle ["EUR" "USD"])
           date-registered (time-coerce/to-long (time/minus (time/now) (time/days (rand-int 60))))
           from (rand-int 100)
           bio (subs lorem from (+ 100 from))
           [professional-title _] (shuffle ["Dr" "Md" "PhD" "Mgr" "Master of Wine and Whisky"])]
       (<? (ethlance-db/insert-row! conn :Users {:user/address address
                                                 :user/type (case address-owner
                                                              "EMPLOYER" :employer
                                                              "CANDIDATE" :candidate
                                                              "ARBITER" :arbiter)
                                                 :user/country country-code
                                                 :user/name (str first-name " " second-name)
                                                 :user/email (string/lower-case (str first-name "@" second-name "." extension))
                                                 :user/profile-image (str "https://randomuser.me/api/portraits/lego/" profile-id ".jpg")
                                                 :user/date-registered date-registered
                                                 :user/date-updated date-registered}))

       (when (= "EMPLOYER" address-owner)
         (<? (ethlance-db/insert-row! conn :Employer {:user/address address
                                                      :employer/bio bio
                                                      :employer/professional-title professional-title})))

       (when (= "CANDIDATE" address-owner)
         (<? (ethlance-db/insert-row! conn :Candidate {:user/address address
                                                       :candidate/rate (rand-int 200)
                                                       :candidate/rate-currency-id currency
                                                       :candidate/bio bio
                                                       :candidate/professional-title professional-title})))
       (when (= "ARBITER" address-owner)
         (<? (ethlance-db/insert-row! conn :Arbiter {:user/address address
                                                     :arbiter/bio bio
                                                     :arbiter/professional-title professional-title
                                                     :arbiter/fee (rand-int 200)
                                                     :arbiter/fee-currency-id currency})))))))

(defn generate-jobs [conn jobs [employer & _]]
  (safe-go
   (doseq [{:keys [job-id job-type]} jobs]
     (let [title (str (-> ["marmot" "deer" "mammut" "tiger" "lion" "elephant" "bobcat"] shuffle first) " "
                      (-> ["control" "design" "programming" "aministartion" "development"] shuffle first))
           description (let [from (rand-int 100)] (subs lorem from (+ 20 from)))
           category (get job-categories (rand-int 13))
           status  (rand-nth ["hiring" "hiring done"])
           date-created (time/minus (time/now) (time/days (rand-int 60)))
           date-published (time/plus date-created (time/days (rand-int 5)))
           date-updated (time/plus date-published (time/days (rand-int 7)))
           expertise-level (rand-int 5)
           token "0x8f389F672Ef0586628689f9227e1d0e09f9A3245"
           reward (rand-int 300)
           web-reference-url (str "http://ethlance.com/" job-id)
           estimated-length (case (-> (rand-nth [:hours :days :weeks]))
                              :hours (time/hours (rand-int 24))
                              :days (time/days (rand-int 30))
                              :weeks (time/weeks (rand-int 100)))
           ;; availability (rand-nth ["Part Time" "Full Time"])
           ;; bid-option (rand-nth ["Hourly Rate" "Bounty"])
           ;; number-of-candidates (rand-int 5)
           ;; invitation-only? (rand-nth [true false])

           language (rand-nth languages)
           job {:job/id job-id
                :job/title title
                :job/description description
                :job/category category
                :job/status status
                :job/date-created (time-coerce/to-long date-created)
                :job/date-published (time-coerce/to-long date-published)
                :job/date-updated (time-coerce/to-long date-updated)
                :job/expertise-level expertise-level
                :job/token token
                :job/token-version 0 ; FIXME: these events don't exist anymore. Remove
                :job/reward reward
                :job/web-reference-url web-reference-url
                :job/language-id language}]

       (case job-type
         :standard-bounties
         (let [bounty-id job-id ;; this is not real but will work for generator
               platform (rand-nth ["mobile" "web" "embedded"])
               date-deadline (time/plus date-updated estimated-length)
               bounty {:standard-bounty/id bounty-id
                       :standard-bounty/platform platform
                       :standard-bounty/deadline (time-coerce/to-long date-deadline)}]
           (<? (ethlance-db/add-bounty conn (merge job bounty))))

         :ethlance-job
         (let [ethlance-job-id job-id
               ethlance-job {:ethlance-job/id ethlance-job-id
                             :ethlance-job/estimated-lenght (time/in-millis estimated-length)
                             :ethlance-job/max-number-of-candidates (rand-int 10)
                             :ethlance-job/invitation-only? (rand-nth [true false])
                             :ethlance-job/required-availability (rand-nth [true false])
                             :ethlance-job/hire-address nil
                             :ethlance-job/bid-option 1}]
           (<? (ethlance-db/add-ethlance-job conn (merge job ethlance-job)))))

       (<? (ethlance-db/insert-row! conn :JobCreator {:job/id job-id
                                                      :user/address employer}))))))

(defn generate-job-arbiters [conn job-ids [_ _ arbiter]]
  (safe-go
   (doseq [job-id job-ids]
     (let [status (rand-nth ["invited" "accepted" ])
           fee (rand-int 200)
           fee-currency-id (rand-nth ["EUR" "USD"])
           arbiter {:job/id job-id
                    :user/address arbiter
                    :job-arbiter/fee fee
                    :job-arbiter/fee-currency-id fee-currency-id
                    :job-arbiter/status status}]
       (<? (ethlance-db/insert-row! conn :JobArbiter arbiter))))))

(defn generate-message [{:message/keys [text] :as message}]
  (-> message
      (merge {:message/text (or text
                                (let [from (rand-int 200)]
                                  (subs lorem from (+ 20 from))))
              :message/date-created (time-coerce/to-long (time/minus (time/now)
                                                                     (time/days (rand-int 60))))})
      (merge (case (:message/type message)
               :job-story-message (case (:job-story-message/type message)
                                    :raise-dispute {}
                                    :resolve-dispute {}
                                    :proposal {}
                                    :invitation {}
                                    :invoice {:invoice/status (rand-nth ["waiting" "paid"])
                                              :invoice/amount-requested (rand-int 10)}
                                    :feedback {:feedback/rating (rand-int 5)}
                                    nil)
               :direct-message {}))))

(defn generate-job-stories [conn stories-ids jobs [employer candidate _]]
  (safe-go
   (doseq [story-id stories-ids]
     (let [_ (println "Generating story" story-id)
           {:keys [job-id job-type]} (rand-nth jobs)
           status  (rand-nth ["proposal pending" "active" "finished" "cancelled"])
           date-created (time/minus (time/now) (time/days (rand-int 60)))
           job-story {:job-story/id story-id
                      :job/id job-id
                      :job-story/status status
                      :job-story/date-created (time-coerce/to-long date-created)
                      :job-story/creator candidate}]

       (case job-type
         :standard-bounties
         (<? (ethlance-db/add-job-story conn job-story))

         :ethlance-job
         (do
           (<? (ethlance-db/add-ethlance-job-story conn job-story))
           (<? (ethlance-db/add-message conn (generate-message {:message/creator employer
                                                                :message/text "Do you want to work with us?"
                                                                :message/type :job-story-message
                                                                :job-story-message/type :invitation
                                                                :job-story/id story-id})))))))))

(defn generate-disputes [conn stories-ids [employer _ _]]
  (safe-go
   (doseq [story-id stories-ids]
     (when (rand-nth [true false])
       (prn "Generating dispute for story " story-id)
       (<? (ethlance-db/add-message conn (generate-message {:message/creator employer
                                                            :message/type :job-story-message
                                                            :job-story-message/type :raise-dispute
                                                            :job-story/id story-id})))
       (<? (ethlance-db/add-message conn (generate-message {:message/creator employer
                                                            :message/type :job-story-message
                                                            :job-story-message/type :resolve-dispute
                                                            :job-story/id story-id})))))))

(defn generate-invoices [conn stories-ids [_ candidate _]]
  (safe-go
   (doseq [story-id stories-ids]
     (let [[status _] (rand-nth ["paid" "pending"])
           date-work-started (time/minus (time/now) (time/days (rand-int 60)))
           work-duration (case (-> [:hours :days :weeks] shuffle first)
                           :hours (time/hours (rand-int 24))
                           :days (time/days (rand-int 30))
                           :weeks (time/weeks (rand-int 100)))
           date-work-ended (time/plus date-work-started work-duration)
           date-paid (when (= "paid" status) (time-coerce/to-long (time/plus date-work-ended (time/days (rand-int 7)))))]
       (<? (ethlance-db/add-message conn (generate-message {:message/creator candidate
                                                            :message/type :job-story-message
                                                            :job-story-message/type :invoice
                                                            :job-story/id story-id
                                                            :invoice/status status
                                                            :invoice/amount-requested (rand-int 12000)
                                                            :invoice/amount-paid (rand-int 12000)
                                                            :invoice/date-work-started (time-coerce/to-long date-work-started)
                                                            :invoice/work-duration (time/in-millis work-duration)
                                                            :invoice/date-work-ended (time-coerce/to-long date-work-ended)
                                                            :invoice/date-paid date-paid})))))))

(defn generate-feedback [conn stories-ids [employer candidate _]]
  (safe-go
   (doseq [story-id stories-ids]
     ;; feedback from the employer to the candidate
     (<? (ethlance-db/add-message conn (generate-message {:message/creator employer
                                                          :message/type :job-story-message
                                                          :job-story-message/type :feedback
                                                          :job-story/id story-id
                                                          :feedback/rating (rand-int 5)
                                                          :user/address candidate})))

     ;; feedback from the candidate to the employer
     (<? (ethlance-db/add-message conn (generate-message {:message/creator candidate
                                                          :message/type :job-story-message
                                                          :job-story-message/type :feedback
                                                          :job-story/id story-id
                                                          :feedback/rating (rand-int 5)
                                                          :user/address employer}))))))
(defn generate-dev-data
  ([conn] (generate-dev-data conn {}))
  ([conn provided-addresses]
   (safe-go
     (let [default-user-types ["EMPLOYER" "CANDIDATE" "ARBITER"]
           user-addresses (map #(or (get provided-addresses %) %) default-user-types)
           user-address-map (into {} (map vector default-user-types user-addresses))
           categories (take 3 constants/categories)
           skills ["Solidity" "Clojure"]
           jobs (map (fn [jid jtype] {:job-id jid :job-type jtype})
                     (range 0 3)
                     (cycle [:standard-bounties :ethlance-job]))
           stories-ids (range 0 5)]
       (<? (generate-users conn user-address-map))
       (<? (generate-categories conn categories user-addresses))
       (<? (generate-skills conn skills user-addresses))
       (<? (generate-user-languages conn user-addresses))
       (<? (generate-jobs conn jobs user-addresses))
       (<? (generate-job-arbiters conn (map :job-id jobs) user-addresses))
       (<? (generate-job-stories conn stories-ids jobs user-addresses))
       (<? (generate-disputes conn stories-ids user-addresses))
       (<? (generate-invoices conn stories-ids user-addresses))
       (<? (generate-feedback conn stories-ids user-addresses))
       (log/debug "Done")))))

(defn generate-for-address
  "Helper function to create dev data for your real Ethereum address"
  [address]
  (go
    (let [conn (<? (async-db/get-connection))
          address-map {"EMPLOYER" address "CANDIDATE" address "ARBITER" address}]
       (<! (generate-dev-data conn address-map)))))

