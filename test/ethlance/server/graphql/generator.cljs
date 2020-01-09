(ns ethlance.server.graphql.generator
  (:require

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

(defn generate-dev-data []
  (let [user-addresses ["EMPLOYER" "CANDIDATE" "ARBITER"]
        categories ["Web" "Mobile" "Embedded"]
        skills ["Solidity" "Clojure"]
        job-ids (map str (range 0 3))
        contract-ids (map str (range 0 5))
        invoice-ids (map str (range 0 10))]
    (promise->
     (generate-users user-addresses)
     ;; #(generate-categories categories user-addresses)
     ;; #(generate-skills skills user-addresses)
     ;; #(generate-user-languages user-addresses)
     ;; #(generate-jobs job-ids user-addresses)
     ;; #(generate-job-arbiters job-ids user-addresses)
     ;; #(generate-contracts contract-ids job-ids user-addresses)
     ;; #(generate-disputes contract-ids job-ids user-addresses)
     ;; #(generate-invoices invoice-ids contract-ids user-addresses)
     ;; #(generate-feedback contract-ids user-addresses)
     #(log/debug "Done"))))
