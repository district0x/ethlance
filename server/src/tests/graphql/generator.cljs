(ns tests.graphql.generator
  (:require
    [cljs-time.coerce :as time-coerce]
    [cljs-time.core :as time]
    [cljs.core.async :refer [go <!]]
    [clojure.string :as string]
    [district.server.async-db :as async-db]
    [district.shared.async-helpers :refer [safe-go <?]]
    [ethlance.server.db :as ethlance-db]
    [ethlance.shared.constants :as constants]
    [taoensso.timbre :as log]))


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


(def languages (map (zipmap constants/languages constants/languages) ["English" "Estonian"]))


(defn generate-user-languages
  [conn user-addresses]
  (safe-go
    (doseq [address user-addresses language languages]
      (let [[speaks? _] (shuffle [true false])]
        (when speaks?
          (<? (ethlance-db/insert-row! conn :UserLanguage {:user/id address
                                                           :language/id language})))))))


(defn generate-categories
  [conn categories [_ candidate arbiter]]
  (safe-go
    (doseq [category categories]
      (<? (ethlance-db/insert-row! conn :Category {:category/id category}))

      (<? (ethlance-db/insert-row! conn :CandidateCategory {:user/id candidate
                                                            :category/id category}))

      (<? (ethlance-db/insert-row! conn :ArbiterCategory {:user/id arbiter
                                                          :category/id category})))))


(defn generate-skills
  [conn skills [_ candidate arbiter]]
  (safe-go
    (doseq [skill skills]
      (<? (ethlance-db/insert-row! conn :Skill {:skill/id skill}))

      (<? (ethlance-db/insert-row! conn :CandidateSkill {:user/id candidate
                                                         :skill/id skill}))

      (<? (ethlance-db/insert-row! conn :ArbiterSkill {:user/id arbiter
                                                       :skill/id skill})))))


(defn generate-users
  [conn user-addresses]
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
        (<? (ethlance-db/insert-row! conn :Users {:user/id address
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
          (<? (ethlance-db/insert-row! conn :Employer {:user/id address
                                                       :employer/bio bio
                                                       :employer/professional-title professional-title})))

        (when (= "CANDIDATE" address-owner)
          (<? (ethlance-db/insert-row! conn :Candidate {:user/id address
                                                        :candidate/rate (rand-int 200)
                                                        :candidate/rate-currency-id currency
                                                        :candidate/bio bio
                                                        :candidate/professional-title professional-title})))
        (when (= "ARBITER" address-owner)
          (<? (ethlance-db/insert-row! conn :Arbiter {:user/id address
                                                      :arbiter/bio bio
                                                      :arbiter/professional-title professional-title
                                                      :arbiter/fee (rand-int 200)
                                                      :arbiter/fee-currency-id currency})))))))


(defn generate-jobs
  [conn jobs [employer & _]]
  (safe-go
    (doseq [{:keys [job-id]} jobs]
      (let [title (str (-> ["marmot" "deer" "mammut" "tiger" "lion" "elephant" "bobcat"] shuffle first) " "
                       (-> ["control" "design" "programming" "aministartion" "development"] shuffle first))
            description (let [from (rand-int 100)] (subs lorem from (+ 20 from)))
            category (get job-categories (rand-int 13))
            status  (rand-nth ["hiring" "hiring done"])
            date-created (time/minus (time/now) (time/days (rand-int 60)))
            date-updated (time/plus date-created (time/days (rand-int 7)))
            expertise-level (rand-int 5)
            token-type :eth
            token-amount 1000
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
                 :job/date-updated (time-coerce/to-long date-updated)
                 :job/expertise-level expertise-level
                 :job/token-type token-type
                 :job/token-amount token-amount
                 :job/language-id language}
            ethlance-job-id job-id
            ethlance-job {:ethlance-job/id ethlance-job-id
                          :ethlance-job/estimated-lenght (time/in-millis estimated-length)
                          :ethlance-job/invitation-only? (rand-nth [true false])
                          :ethlance-job/required-availability (rand-nth [true false])
                          :ethlance-job/hire-address nil
                          :ethlance-job/bid-option 1}]
        (<? (ethlance-db/add-job conn (merge job ethlance-job)))
        (<? (ethlance-db/insert-row! conn :JobCreator {:job/id job-id
                                                       :user/id employer}))))))


(defn generate-job-arbiters
  [conn job-ids [_ _ arbiter]]
  (safe-go
    (doseq [job-id job-ids]
      (let [status (rand-nth ["invited" "accepted"])
            fee (rand-int 200)
            fee-currency-id (rand-nth ["EUR" "USD"])
            arbiter {:job/id job-id
                     :user/id arbiter
                     :job-arbiter/fee fee
                     :job-arbiter/fee-currency-id fee-currency-id
                     :job-arbiter/status status}]
        (<? (ethlance-db/insert-row! conn :JobArbiter arbiter))))))


(defn generate-message
  [{:message/keys [text] :as message}]
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


(defn generate-job-stories
  [conn stories-ids jobs [employer candidate _]]
  (safe-go
    (doseq [story-id stories-ids]
      (let [{:keys [job-id]} (rand-nth jobs)
            status  (rand-nth ["proposal pending" "active" "finished" "cancelled"])
            date-created (time/minus (time/now) (time/days (rand-int 60)))
            job-story {:job-story/id story-id
                       :job/id job-id
                       :job-story/status status
                       :job-story/date-created (time-coerce/to-long date-created)
                       :job-story/creator candidate}]
        (<? (ethlance-db/add-job-story conn job-story))
        (<? (ethlance-db/add-message conn (generate-message {:message/creator employer
                                                             :message/text "Do you want to work with us?"
                                                             :message/type :job-story-message
                                                             :job-story-message/type :invitation
                                                             :job-story/id story-id})))))))


(defn generate-disputes
  [conn stories-ids [employer _candidate arbiter]]
  (safe-go
    (doseq [story-id stories-ids]
      (when (rand-nth [true false])
        (<? (ethlance-db/add-message conn (generate-message {:message/creator employer
                                                             :message/type :job-story-message
                                                             :job-story-message/type :raise-dispute
                                                             :job-story/id story-id})))
        (<? (ethlance-db/add-message conn (generate-message {:message/creator arbiter
                                                             :message/type :job-story-message
                                                             :job-story-message/type :resolve-dispute
                                                             :job-story/id story-id})))))))


(defn generate-invoices
  [conn stories-ids [_ candidate _]]
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


(defn generate-feedback
  [conn stories-ids [employer candidate _]]
  (safe-go
    (doseq [story-id stories-ids]
      ;; feedback from the employer to the candidate
      (<? (ethlance-db/add-message conn (generate-message {:message/creator employer
                                                           :message/type :job-story-message
                                                           :job-story-message/type :feedback
                                                           :job-story/id story-id
                                                           :feedback/rating (rand-int 5)
                                                           :feedback/receiver-role "candidate"
                                                           :user/id candidate})))

      ;; feedback from the candidate to the employer
      (<? (ethlance-db/add-message conn (generate-message {:message/creator candidate
                                                           :message/type :job-story-message
                                                           :job-story-message/type :feedback
                                                           :job-story/id story-id
                                                           :feedback/rating (rand-int 5)
                                                           :feedback/receiver-role "employer"
                                                           :user/id employer}))))))


(defn generate-dev-data
  ([conn] (generate-dev-data conn {}))
  ([conn provided-addresses]
   (safe-go
     (let [default-user-types ["EMPLOYER" "CANDIDATE" "ARBITER"]
           user-addresses (map #(or (get provided-addresses %) %) default-user-types)
           user-address-map (into {} (map vector default-user-types user-addresses))
           categories (take 3 constants/categories)
           skills ["Solidity" "Clojure"]
           jobs (map (fn [jid] {:job-id jid}) (range 0 3))
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
