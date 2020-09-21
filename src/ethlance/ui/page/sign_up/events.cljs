(ns ethlance.ui.page.sign-up.events
  (:require [district.parsers :refer [parse-float]]
            [ethlance.ui.util.component :refer [>evt]]
            [district.ui.web3-accounts.events :as accounts-events]
            [district.ui.web3-accounts.queries :as accounts-queries]
            [district.ui.router.effects :as router.effects]
            [ethlance.ui.event.utils :as event.utils]
            [ethlance.ui.graphql :as graphql]
            [re-frame.core :as re]
            [taoensso.timbre :as log]))

(def state-key :page.sign-up)

(def create-assoc-handler (partial event.utils/create-assoc-handler state-key))

(re/reg-event-fx
 :page.sign-up/set-user-name
 (fn [{:keys [db]} [_ name]]
   (let [address (accounts-queries/active-account db)]
     {:db (-> db (assoc-in [:users address :user/user-name] name))})))

(re/reg-event-fx
 :page.sign-up/set-user-email
 (fn [{:keys [db]} [_ email]]
   (let [address (accounts-queries/active-account db)]
     {:db (-> db (assoc-in [:users address :user/email] email))})))

(re/reg-event-fx
 :page.sign-up/set-user-country-code
 (fn [{:keys [db]} [_ country-code]]
   (let [address (accounts-queries/active-account db)]
     {:db (-> db (assoc-in [:users address :user/country-code] country-code))})))

(re/reg-event-fx
 :page.sign-up/set-user-languages
 (fn [{:keys [db]} [_ languages]]
   (let [address (accounts-queries/active-account db)]
     {:db (-> db (assoc-in [:users address :user/languages] languages))})))

(re/reg-event-fx
 :page.sign-up/set-candidate-professional-title
 (fn [{:keys [db]} [_ professional-title]]
   (let [address (accounts-queries/active-account db)]
     {:db (-> db (assoc-in [:candidates address :candidate/professional-title] professional-title))})))

(re/reg-event-fx
 :page.sign-up/set-candidate-hourly-rate
 (fn [{:keys [db]} [_ rate]]
   (let [address (accounts-queries/active-account db)]
     {:db (-> db (assoc-in [:candidates address :candidate/rate] rate))})))

(re/reg-event-fx
 :page.sign-up/set-candidate-categories
 (fn [{:keys [db]} [_ categories]]
   (let [address (accounts-queries/active-account db)]
     {:db (-> db (assoc-in [:candidates address :candidate/categories] categories))})))

(re/reg-event-fx
 :page.sign-up/set-candidate-skills
 (fn [{:keys [db]} [_ skills]]
   (let [address (accounts-queries/active-account db)]
     {:db (-> db (assoc-in [:candidates address :candidate/skills] skills))})))

(re/reg-event-fx
 :page.sign-up/set-candidate-bio
 (fn [{:keys [db]} [_ bio]]
   (let [address (accounts-queries/active-account db)]
     {:db (-> db (assoc-in [:candidates address :candidate/bio] bio))})))

(re/reg-event-fx
 :page.sign-up/set-candidate-for-hire?
 (fn [{:keys [db]} [_ for-hire?]]
   (let [address (accounts-queries/active-account db)]
     {:db (-> db (assoc-in [:candidates address :candidate/for-hire?] for-hire?))})))

(re/reg-event-fx
 :page.sign-up/set-employer-bio
 (fn [{:keys [db]} [_ bio]]
   (let [address (accounts-queries/active-account db)]
     {:db (-> db (assoc-in [:employers address :employer/bio] bio))})))

(re/reg-event-fx
 :page.sign-up/set-employer-professional-title
 (fn [{:keys [db]} [_ professional-title]]
   (let [address (accounts-queries/active-account db)]
     {:db (-> db (assoc-in [:employers address :employer/professional-title] professional-title))})))

(re/reg-event-fx :page.sign-up/set-arbiter-full-name (create-assoc-handler :arbiter/full-name))
(re/reg-event-fx :page.sign-up/set-arbiter-professional-title (create-assoc-handler :arbiter/professional-title))
(re/reg-event-fx :page.sign-up/set-arbiter-fixed-rate-per-dispute (create-assoc-handler :arbiter/fixed-rate-per-dispute parse-float))
(re/reg-event-fx :page.sign-up/set-arbiter-email (create-assoc-handler :arbiter/email))
(re/reg-event-fx :page.sign-up/set-arbiter-github-key (create-assoc-handler :arbiter/github-key))
(re/reg-event-fx :page.sign-up/set-arbiter-linkedin-key (create-assoc-handler :arbiter/linkedin-key))
(re/reg-event-fx :page.sign-up/set-arbiter-languages (create-assoc-handler :arbiter/languages))
(re/reg-event-fx :page.sign-up/set-arbiter-biography (create-assoc-handler :arbiter/biography))
(re/reg-event-fx :page.sign-up/set-arbiter-country (create-assoc-handler :arbiter/country))

(re/reg-event-fx
 :page.sign-up/initialize-page
 (fn []
   {:forward-events
    {:register ::accounts-loaded?
     :events #{::accounts-events/accounts-changed}
     :dispatch-to [:page.sign-up/initial-query]}}))

(re/reg-event-fx
 :page.sign-up/initial-query
 (fn [{:keys [db]}]
   (let [user-address (accounts-queries/active-account db)]
     {:dispatch [::graphql/query {:query
                                  "query InitialQuery($address: ID!) {
                                     user(user_address: $address) {
                                       user_address
                                       user_userName
                                       user_userName
                                       user_email
                                       user_githubUsername
                                       user_countryCode
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
                                     }
                                   }"
                                  :variables {:address user-address}}]})))

(re/reg-event-fx
 :page.sign-up/send-github-verification-code
 (fn [_ [_ code]]
   {:forward-events
    {:register ::initial-query?
     :events #{:page.sign-up/initial-query}
     :dispatch-to [:page.sign-up/github-sign-up code]}}))

(re/reg-event-fx
 :page.sign-up/github-sign-up
 (fn [{:keys [db]} [_  code]]
   (let [user-address (accounts-queries/active-account db)]
     {:dispatch [::graphql/query {:query
                                  "mutation GithubSignUp($githubSignUpInput: githubSignUpInput!) {
                                     githubSignUp(input: $githubSignUpInput) {
                                       user_address
                                       user_fullName
                                       user_githubUsername
                                       user_email
                                       user_countryCode
                                   }
                                 }"
                                  :variables {:githubSignUpInput {:code code :user_address user-address}}
                                  :on-success #(>evt [::unregister-initial-query-forwarder])}]})))

(re/reg-event-fx
 ::unregister-initial-query-forwarder
 (fn []
   {:forward-events {:unregister ::initial-query?}}))

(re/reg-event-fx
 :page.sign-up/update-candidate
 (fn [{:keys [db]}]
   (let [user-address (accounts-queries/active-account db)
         {:user/keys [user-name github-username country-code email]} (get-in db [:users user-address])
         {:candidate/keys [rate
                           professional-title
                           categories
                           skills
                           bio]}
         (get-in db [:candidates user-address])]
     {:dispatch [::graphql/query {:query
                                  "mutation UpdateCandidate($candidateInput: CandidateInput!) {
                                     updateCandidate(input: $candidateInput) {
                                       user_address
                                       user_dateRegistered
                                       candidate_dateRegistered
                                   }
                                 }"
                                  :variables {:candidateInput {:user_address user-address
                                                               :user_email email
                                                               :user_userName user-name
                                                               :user_githubUsername github-username
                                                               :user_countryCode country-code
                                                               :candidate_bio bio
                                                               :candidate_professionalTitle professional-title
                                                               :candidate_categories categories
                                                               :candidate_skills skills
                                                               :candidate_rate (js/parseInt rate)
                                                               ;; NOTE: hardcoded since UI does not allow for a different currency
                                                               :candidate_rateCurrencyId :USD}}}]})))
