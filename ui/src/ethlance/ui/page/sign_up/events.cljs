(ns ethlance.ui.page.sign-up.events
  (:require
    [district.parsers :as parsers]
    [district.ui.logging.events :as logging]
    [district.ui.web3-accounts.events :as accounts-events]
    [district.ui.web3-accounts.queries :as accounts-queries]
    [ethlance.ui.event.utils :as event.utils]
    [ethlance.ui.graphql :as graphql]
    [ethlance.ui.util.component :refer [>evt]]
    [re-frame.core :as re]))

(def state-key :page.sign-up)

(def create-assoc-handler (partial event.utils/create-assoc-handler state-key))

(re/reg-event-fx :page.sign-up/set-user-name (create-assoc-handler :user/name))
(re/reg-event-fx :page.sign-up/set-user-email (create-assoc-handler :user/email))
(re/reg-event-fx :page.sign-up/set-user-country-code (create-assoc-handler :user/country))
(re/reg-event-fx :page.sign-up/set-user-languages (create-assoc-handler :user/languages))
(re/reg-event-fx :page.sign-up/set-user-profile-image (create-assoc-handler :user/profile-image))
(re/reg-event-fx :page.sign-up/set-candidate-professional-title (create-assoc-handler :candidate/professional-title))
(re/reg-event-fx :page.sign-up/set-candidate-rate (create-assoc-handler :candidate/rate))
(re/reg-event-fx :page.sign-up/set-candidate-categories (create-assoc-handler :candidate/categories))
(re/reg-event-fx :page.sign-up/set-candidate-skills (create-assoc-handler :candidate/skills))
(re/reg-event-fx :page.sign-up/set-candidate-bio (create-assoc-handler :candidate/bio))
(re/reg-event-fx :page.sign-up/set-employer-professional-title (create-assoc-handler :employer/professional-title))
(re/reg-event-fx :page.sign-up/set-employer-bio (create-assoc-handler :employer/bio))
(re/reg-event-fx :page.sign-up/set-arbiter-fee (create-assoc-handler :arbiter/fee))
(re/reg-event-fx :page.sign-up/set-arbiter-professional-title (create-assoc-handler :arbiter/professional-title))
(re/reg-event-fx :page.sign-up/set-arbiter-bio (create-assoc-handler :arbiter/bio))


(def interceptors [re/trim-v])


(re/reg-event-fx
  :page.sign-up/initialize-page
  (fn []
    {:forward-events
     {:register ::accounts-loaded?
      :events #{::accounts-events/accounts-changed}
      :dispatch-to [:page.sign-up/initial-query]}}))

(re/reg-event-fx
  :page.sign-up/initial-query
  [interceptors]
  (fn [{:keys [db]}]
    (let [user-address (accounts-queries/active-account db)]
      {:dispatch [::graphql/query {:query
                                   "query InitialQuery($address: ID!) {
                                      user(user_address: $address) {
                                        user_address
                                        user_name
                                        user_email
                                        user_githubUsername
                                        user_country
                                        user_isRegisteredCandidate
                                        user_languages
                                      }
                                      candidate(user_address: $address) {
                                        user_address
                                        candidate_professionalTitle
                                        candidate_rate
                                        candidate_rateCurrencyId
                                        candidate_skills
                                        candidate_bio
                                        candidate_categories
                                      }
                                      employer(user_address: $address) {
                                        user_address
                                        employer_professionalTitle
                                        employer_bio
                                      }
                                      arbiter(user_address: $address) {
                                        user_address
                                        arbiter_bio
                                        arbiter_professionalTitle
                                        arbiter_fee
                                      }
                                    }"
                                   :variables {:address user-address}}]})))

(re/reg-event-fx
  :page.sign-up/send-github-verification-code
  [interceptors]
  (fn [_ [code]]
    {:forward-events
     {:register ::initial-query?
      :events #{:page.sign-up/initial-query}
      :dispatch-to [:page.sign-up/github-sign-up code]}}))

(re/reg-event-fx
  :page.sign-up/github-sign-up
  [interceptors]
  (fn [{:keys [db]} [code]]
    (let [user-address (accounts-queries/active-account db)]
      {:dispatch [::graphql/query {:query
                                   "mutation GithubSignUp($githubSignUpInput: githubSignUpInput!) {
                                      githubSignUp(input: $githubSignUpInput) {
                                        user_address
                                        user_name
                                        user_githubUsername
                                        user_email
                                        user_country
                                    }
                                  }"
                                   :variables {:githubSignUpInput {:code code :user_address user-address}}
                                   :on-success #(>evt [::unregister-initial-query-forwarder])}]})))


(re/reg-event-fx
  :page.sign-up/send-linkedin-verification-code
  [interceptors]
  (fn [_ [code redirect-uri]]
    {:forward-events
     {:register ::initial-query?
      :events #{:page.sign-up/initial-query}
      :dispatch-to [:page.sign-up/linkedin-sign-up code redirect-uri]}}))

(re/reg-event-fx
  :page.sign-up/linkedin-sign-up
  [interceptors]
  (fn [{:keys [db]} [code redirect-uri]]
    (let [user-address (accounts-queries/active-account db)]
      {:dispatch [::graphql/query {:query
                                   "mutation LinkedinSignUp($linkedinSignUpInput: linkedinSignUpInput!) {
                                      linkedinSignUp(input: $linkedinSignUpInput) {
                                        user_address
                                        user_name
                                        user_linkedinUsername
                                        user_email
                                        user_country
                                    }
                                  }"
                                   :variables {:linkedinSignUpInput {:code code :user_address user-address :redirectUri redirect-uri}}
                                   :on-success #(>evt [::unregister-initial-query-forwarder])}]})))

(re/reg-event-fx
  ::unregister-initial-query-forwarder
  (fn []
    {:forward-events {:unregister ::initial-query?}}))

(defn- fallback-data [db section address]
  (merge (get-in db [:users address]) (get-in db [section address])))

(re/reg-event-fx
  :page.sign-up/update-candidate
  [interceptors]
  (fn [{:keys [db]}]
    (let [user-address (accounts-queries/active-account db)
          {:keys [:user/email
                  :user/country
                  :user/name
                  :user/languages
                  ; :user/profile-image
                  ; :user/github-code
                  ; :user/linkedin-code
                  ; :user/linkedin-redirect-uri
                  :candidate/professional-title
                  :candidate/rate
                  :candidate/categories
                  :candidate/bio
                  :candidate/skills
                  ]} (merge (fallback-data db :candidates user-address) (get-in db [state-key]))]
      {:dispatch [::graphql/query {:query
                                   "mutation UpdateCandidate($candidateInput: CandidateInput!) {
                                      updateCandidate(input: $candidateInput) {
                                        user_address
                                        user_dateUpdated
                                        candidate_dateUpdated
                                    }
                                  }"
                                   :variables {:candidateInput {:user_address user-address
                                                                :user_email email
                                                                :user_name name
                                                                :user_country country
                                                                :user_languages languages
                                                                :candidate_bio bio
                                                                :candidate_professionalTitle professional-title
                                                                :candidate_categories categories
                                                                :candidate_skills skills
                                                                :candidate_rate (parsers/parse-int rate)
                                                                ;; NOTE: hardcoded since UI does not allow for a different currency
                                                                :candidate_rateCurrencyId :USD}}}]})))

(re/reg-event-fx
  :page.sign-up/update-employer
  [interceptors]
  (fn [{:keys [db]}]
    (let [user-address (accounts-queries/active-account db)
          {:keys [:user/name
                  :user/email
                  :user/languages
                  :user/github-username
                  :user/country
                  :employer/professional-title
                  :employer/bio]} (merge (fallback-data db :employers user-address) (get-in db [state-key]))]
      {:dispatch [::graphql/query {:query
                                   "mutation UpdateEmployer($employerInput: EmployerInput!) {
                                      updateEmployer(input: $employerInput) {
                                        user_address
                                        user_dateUpdated
                                        employer_dateUpdated
                                    }
                                  }"
                                   :variables {:employerInput {:user_address user-address
                                                               :user_email email
                                                               :user_name name
                                                               :user_githubUsername github-username
                                                               :user_country country
                                                               :user_languages languages
                                                               :employer_bio bio
                                                               :employer_professionalTitle professional-title}}}]})))


(re/reg-event-fx
  :page.sign-up/update-arbiter
  [interceptors]
  (fn [{:keys [db]}]
    (let [user-address (accounts-queries/active-account db)
          {:keys [:user/name
                  :user/email
                  :user/languages
                  :user/github-username
                  :user/country
                  :arbiter/professional-title
                  :arbiter/bio
                  :arbiter/fee]} (merge (fallback-data db :arbiters user-address) (get-in db [state-key]))]
      {:dispatch [::graphql/query {:query
                                   "mutation UpdateArbiter($arbiterInput: ArbiterInput!) {
                                      updateArbiter(input: $arbiterInput) {
                                        user_address
                                        user_dateUpdated
                                        arbiter_dateUpdated
                                    }
                                  }"
                                   :variables {:arbiterInput {:user_address user-address
                                                              :user_email email
                                                              :user_name name
                                                              :user_githubUsername github-username
                                                              :user_country country
                                                              :user_languages languages
                                                              :arbiter_bio bio
                                                              :arbiter_professionalTitle professional-title
                                                              :arbiter_fee (js/parseInt fee)
                                                              ;; NOTE: hardcoded since UI does not allow for a different currency
                                                              :arbiter_feeCurrencyId :USD}}}]})))


(re/reg-event-fx
  :page.sign-up/upload-user-image
  [interceptors]
  (fn [_ [{:keys [:file-info] :as data}]]
    {:ipfs/call {:func "add"
                 :args [(:file file-info)]
                 :on-success [::upload-user-image-success data]
                 :on-error [::logging/error "Error uploading user image" {:data data}]}}))


(re/reg-event-fx
  ::upload-user-image-success
  [interceptors]
  (fn [_ [_ ipfs-resp]]
    {:dispatch [:page.sign-up/set-user-profile-image (get (js->clj (js/JSON.parse ipfs-resp)) "Hash")]}))

