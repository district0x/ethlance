(ns ethlance.db
  (:require [cljs-web3.core :as web3]
            [cljs-time.core :as t]
            [cljs.spec.alpha :as s]
            [ethlance.constants :as constants]
            [ethlance.utils :as u]
            [re-frame.core :refer [dispatch]]))

(s/def ::load-node-addresses? boolean?)
(s/def ::active-setters? boolean?)
(s/def ::web3 (complement nil?))
(s/def ::node-url string?)
(s/def ::provides-web3? boolean?)
(s/def ::contracts-not-found? boolean?)
(s/def ::last-transaction-gas-used (s/nilable number?))
(s/def ::drawer-open? boolean?)
(s/def ::search-freelancers-filter-open? boolean?)
(s/def ::search-jobs-filter-open? boolean?)
(s/def ::skills-loaded? boolean?)
(s/def ::handler keyword?)
(s/def ::route-params (s/map-of keyword? (some-fn number? string?)))
(s/def :window/width-size int?)
(s/def ::active-page (s/keys :req-un [::handler] :opt-un [::route-params]))
(s/def ::selected-currency (partial contains? (set (keys constants/currencies))))
(s/def ::open? boolean?)
(s/def ::message string?)
(s/def ::on-request-close fn?)
(s/def ::auto-hide-duration int?)
(s/def ::title string?)
(s/def ::modal boolean?)
(s/def ::snackbar (s/keys :req-un [::open? ::message ::on-request-close ::auto-hide-duration]))
(s/def ::dialog (s/keys :req-un [::open? ::title ::modal]))
(s/def :eth/config (s/map-of keyword? int?))
(s/def ::name string?)
(s/def ::address string?)
(s/def ::bin string?)
(s/def ::abi array?)
(s/def ::setter? boolean?)
(s/def :eth/contracts (s/map-of keyword? (s/keys :req-un [::name] :opt-un [::setter? ::address ::bin ::abi])))
(s/def ::my-addresses (s/coll-of string?))
(s/def ::active-address (s/nilable string?))
(s/def ::my-users-loaded? boolean?)
(s/def :blockchain/connection-error? boolean?)
(s/def ::conversion-rates (s/map-of number? number?))
(s/def ::conversion-rates-historical (s/map-of number? ::conversion-rates))
(s/def ::load-all-conversion-rates-interval (s/nilable int?))
(s/def ::legacy-user-ids (s/map-of pos? (s/keys :opt [:user/address])))

(s/def :user/id u/address?)
(s/def :employer/avg-rating u/uint8?)
(s/def :employer/contracts-count u/uint?)
(s/def :employer/description u/string-or-nil?)
(s/def :employer/jobs u/uint-coll?)
(s/def :employer/jobs-count u/uint?)
(s/def :employer/ratings-count u/uint?)
(s/def :employer/total-invoiced u/big-num?)
(s/def :employer/total-paid u/big-num?)
(s/def :freelancer/available? boolean?)
(s/def :freelancer/avg-rating u/uint8?)
(s/def :freelancer/categories u/uint-coll?)
(s/def :freelancer/categories-count u/uint?)
(s/def :freelancer/contracts u/uint-coll?)
(s/def :freelancer/contracts-count u/uint?)
(s/def :freelancer/description u/string-or-nil?)
(s/def :freelancer/hourly-rate u/big-num|num|str?)
(s/def :freelancer/hourly-rate-currency u/uint8?)
(s/def :freelancer/job-title u/string-or-nil?)
(s/def :freelancer/ratings-count u/uint?)
(s/def :freelancer/skills u/uint-coll?)
(s/def :freelancer/skills-count u/uint?)
(s/def :freelancer/total-earned u/big-num?)
(s/def :freelancer/total-invoiced u/big-num?)
(s/def :user.notif/disabled-all? boolean?)
(s/def :user.notif/disabled-newsletter? boolean?)
(s/def :user.notif/disabled-on-invoice-added? boolean?)
(s/def :user.notif/disabled-on-invoice-paid? boolean?)
(s/def :user.notif/disabled-on-job-contract-added? boolean?)
(s/def :user.notif/disabled-on-job-contract-feedback-added? boolean?)
(s/def :user.notif/disabled-on-job-invitation-added? boolean?)
(s/def :user.notif/disabled-on-job-proposal-added? boolean?)
(s/def :user.notif/disabled-on-message-added? boolean?)
(s/def :user.notif/disabled-on-job-sponsorship-added? boolean?)
(s/def :user.notif/job-recommendations u/uint8?)
(s/def :user/address u/address?)
(s/def :user/balance u/big-num?)
(s/def :user/country u/uint?)
(s/def :user/created-on u/date-or-nil?)
(s/def :user/email u/string-or-nil?)
(s/def :user/employer? boolean?)
(s/def :user/freelancer? boolean?)
(s/def :user/github u/string-or-nil?)
(s/def :user/gravatar u/bytes32?)
(s/def :user/languages u/uint-coll?)
(s/def :user/languages-count u/uint?)
(s/def :user/linkedin u/string-or-nil?)
(s/def :user/name u/string-or-nil?)
(s/def :user/state u/uint?)
(s/def :user/status u/uint8?)
(s/def :user/sponsorships u/uint-coll?)
(s/def :user/sponsorships-count u/uint?)

(s/def :app/user (s/keys :opt [:user/id
                               :user/address                ;; backward compatibility
                               :user/balance
                               :user/country
                               :user/created-on
                               :user/email
                               :user/employer?
                               :user/freelancer?
                               :user/github
                               :user/gravatar
                               :user/languages
                               :user/languages-count
                               :user/linkedin
                               :user/name
                               :user/state
                               :user/status
                               :user/sponsorships
                               :user/sponsorships-count
                               :user.notif/disabled-all?
                               :user.notif/disabled-newsletter?
                               :user.notif/disabled-on-invoice-added?
                               :user.notif/disabled-on-invoice-paid?
                               :user.notif/disabled-on-job-contract-added?
                               :user.notif/disabled-on-job-contract-feedback-added?
                               :user.notif/disabled-on-job-invitation-added?
                               :user.notif/disabled-on-job-proposal-added?
                               :user.notif/disabled-on-message-added?
                               :user.notif/disabled-on-job-sponsorship-added?
                               :user.notif/job-recommendations
                               :freelancer/available?
                               :freelancer/avg-rating
                               :freelancer/categories
                               :freelancer/categories-count
                               :freelancer/contracts
                               :freelancer/contracts-count
                               :freelancer/description
                               :freelancer/hourly-rate
                               :freelancer/hourly-rate-currency
                               :freelancer/job-title
                               :freelancer/ratings-count
                               :freelancer/skills
                               :freelancer/skills-count
                               :freelancer/total-earned
                               :freelancer/total-invoiced
                               :employer/avg-rating
                               :employer/description
                               :employer/jobs
                               :employer/jobs-count
                               :employer/ratings-count
                               :employer/total-invoiced
                               :employer/total-paid]))
(s/def :app/users (s/map-of pos? :app/user))

(s/def :job/id u/uint?)
(s/def :job/budget u/big-num|num|str?)
(s/def :job/category u/uint8?)
(s/def :job/contracts u/uint-coll?)
(s/def :job/contracts-count u/uint?)
(s/def :job/created-on u/date?)
(s/def :job/description string?)
(s/def :job/employer :user/id)
(s/def :job/estimated-duration u/uint8?)
(s/def :job/experience-level u/uint8?)
(s/def :job/freelancers-needed u/uint8?)
(s/def :job/reference-currency u/uint8?)
(s/def :job/hiring-done-on u/date-or-nil?)
(s/def :job/hours-per-week u/uint8?)
(s/def :job/language u/uint?)
(s/def :job/payment-type u/uint8?)
(s/def :job/skills u/uint-coll?)
(s/def :job/skills-count u/uint?)
(s/def :job/status u/uint8?)
(s/def :job/title string?)
(s/def :job/total-paid u/big-num?)
(s/def :job/sponsorable? boolean?)
(s/def :job/invitation-only? boolean?)
(s/def :job/allowed-users u/address-coll?)
(s/def :job/allowed-users-count u/uint?)
(s/def :job/sponsorships-balance u/big-num?)
(s/def :job/sponsorships-total u/big-num?)
(s/def :job/sponsorships-total-refunded u/big-num?)
(s/def :job/sponsorships u/uint-coll?)
(s/def :job/sponsorships-count u/uint?)

(s/def :app/job (s/keys :opt [:job/id
                              :job/budget
                              :job/category
                              :job/contracts
                              :job/contracts-count
                              :job/created-on
                              :job/description
                              :job/employer
                              :job/estimated-duration
                              :job/experience-level
                              :job/freelancers-needed
                              :job/reference-currency
                              :job/hiring-done-on
                              :job/hours-per-week
                              :job/language
                              :job/payment-type
                              :job/skills
                              :job/skills-count
                              :job/sponsorships
                              :job/sponsorships-count
                              :job/sponsorships-balance
                              :job/sponsorships-total
                              :job/sponsorships-total-refunded
                              :job/status
                              :job/title
                              :job/total-paid
                              :job/sponsorable?
                              :job/invitation-only?
                              :job/allowed-users
                              :job/allowed-users-count]))

(s/def :app/jobs (s/map-of pos? :app/job))


(s/def :job.allowed-user/approved? boolean?)
(s/def :app/job.allowed-user (s/keys :opt [:job.allowed-user/approved?]))
(s/def :app/jobs.allowed-users (s/map-of (s/cat :job/id :job/id :user/id :user/id) :app/job.allowed-user))

(s/def :contract/id u/uint?)
(s/def :invitation/created-on u/date-or-nil?)
(s/def :invitation/description u/string-or-nil?)
(s/def :proposal/created-on u/date-or-nil?)
(s/def :proposal/description u/string-or-nil?)
(s/def :proposal/rate u/big-num|num|str?)
(s/def :contract/cancel-description u/string-or-nil?)
(s/def :contract/cancelled-on u/date-or-nil?)
(s/def :contract/created-on u/date-or-nil?)
(s/def :contract/description u/string-or-nil?)
(s/def :contract/done-by-freelancer? boolean?)
(s/def :contract/done-on u/date-or-nil?)
(s/def :contract/freelancer :user/id)
(s/def :contract/invoices u/uint-coll?)
(s/def :contract/invoices-count u/uint?)
(s/def :contract/job :job/id)
(s/def :contract/messages u/uint-coll?)
(s/def :contract/messages-count u/uint?)
(s/def :contract/status u/uint8?)
(s/def :contract/total-invoiced u/big-num?)
(s/def :contract/total-paid u/big-num?)
(s/def :contract/employer-feedback u/string-or-nil?)
(s/def :contract/employer-feedback-on u/date-or-nil?)
(s/def :contract/employer-feedback-rating u/uint8?)
(s/def :contract/freelancer-feedback u/string-or-nil?)
(s/def :contract/freelancer-feedback-on u/date-or-nil?)
(s/def :contract/freelancer-feedback-rating u/uint8?)
(s/def :contract/feedback string?)

(s/def :app/contract (s/keys :opt [:contract/id
                                   :invitation/created-on
                                   :invitation/description
                                   :proposal/created-on
                                   :proposal/description
                                   :proposal/rate
                                   :contract/cancel-description
                                   :contract/cancelled-on
                                   :contract/created-on
                                   :contract/description
                                   :contract/done-by-freelancer?
                                   :contract/done-on
                                   :contract/employer-feedback
                                   :contract/employer-feedback-on
                                   :contract/employer-feedback-rating
                                   :contract/freelancer
                                   :contract/freelancer-feedback
                                   :contract/freelancer-feedback-on
                                   :contract/invoices
                                   :contract/invoices-count
                                   :contract/job
                                   :contract/messages
                                   :contract/messages-count
                                   :contract/status
                                   :contract/total-invoiced
                                   :contract/total-paid
                                   :contract/freelancer-feedback-rating]))

(s/def :app/contracts (s/map-of pos? :app/contract))

(s/def :invoice/id pos?)
(s/def :invoice/amount u/big-num|num|str?)
(s/def :invoice/rate u/big-num|num|str?)
(s/def :invoice/cancelled-on u/date-or-nil?)
(s/def :invoice/contract :contract/id)
(s/def :invoice/conversion-rate u/big-num|num|str?)
(s/def :invoice/created-on u/date?)
(s/def :invoice/description string?)
(s/def :invoice/paid-on u/date-or-nil?)
(s/def :invoice/status u/uint8?)
(s/def :invoice/worked-from u/date?)
(s/def :invoice/worked-hours u/uint?)
(s/def :invoice/worked-minutes u/uint?)
(s/def :invoice/worked-to u/date?)
(s/def :invoice/paid-by :user/id)

(s/def :app/invoice (s/keys :opt [:invoice/id
                                  :invoice/amount
                                  :invoice/rate
                                  :invoice/cancelled-on
                                  :invoice/contract
                                  :invoice/conversion-rate
                                  :invoice/created-on
                                  :invoice/description
                                  :invoice/paid-on
                                  :invoice/status
                                  :invoice/worked-from
                                  :invoice/worked-hours
                                  :invoice/worked-minutes
                                  :invoice/worked-to
                                  :invoice/paid-by]))

(s/def :app/invoices (s/map-of pos? :app/invoice))

(s/def :skill/id pos?)
(s/def :skill/name u/bytes32?)
(s/def :skill/creator :user/id)
(s/def :skill/created-on u/date?)
(s/def :skill/updated-on u/date-or-nil?)
(s/def :skill/jobs-count u/uint?)
(s/def :skill/jobs u/uint-coll?)
(s/def :skill/blocked? boolean?)
(s/def :skill/freelancers-count u/uint?)
(s/def :skill/freelancers u/uint-coll?)

(s/def :app/skill (s/keys :opt [:skill/id
                                :skill/name
                                :skill/creator
                                :skill/created-on
                                :skill/updated-on
                                :skill/jobs-count
                                :skill/jobs
                                :skill/blocked?
                                :skill/freelancers-count
                                :skill/freelancers]))

(s/def :app/skills (s/map-of pos? :app/skill))
(s/def :app/skill-count int?)
(s/def ::skill-load-limit pos?)

(s/def :message/id pos?)
(s/def :message/text string?)
(s/def :message/created-on u/date?)
(s/def :message/receiver :user/id)
(s/def :message/sender :user/id)
(s/def :message/contract :contract/id)
(s/def :message/contract-status u/uint8?)

(s/def :app/message (s/keys :opt [:message/id
                                  :message/text
                                  :message/created-on
                                  :message/receiver
                                  :message/sender
                                  :message/contract
                                  :message/contract-status]))

(s/def :sponsorship/id pos?)
(s/def :sponsorship/user :user/id)
(s/def :sponsorship/created-on u/date?)
(s/def :sponsorship/amount u/big-num|num|str?)
(s/def :sponsorship/job :job/id)
(s/def :sponsorship/name string?)
(s/def :sponsorship/link string?)
(s/def :sponsorship/updated-on u/date?)
(s/def :sponsorship/refunded? boolean?)
(s/def :sponsorship/refunded-amount u/big-num|num|str?)

(s/def :app/sponsorship (s/keys :opt [:sponsorship/id
                                      :sponsorship/user
                                      :sponsorship/created-on
                                      :sponsorship/amount
                                      :sponsorship/job
                                      :sponsorship/name
                                      :sponsorship/link
                                      :sponsorship/updated-on
                                      :sponsorship/refunded?
                                      :sponsorship/refunded-amount]))

(s/def :app/sponsorships (s/map-of pos? :app/sponsorship))

(s/def ::items (s/or :int-ids (s/coll-of (s/nilable int?))
                     :address-ids (s/coll-of (s/nilable u/address?))))
(s/def ::loading? boolean?)
(s/def ::params (s/map-of keyword? any?))
(s/def ::limit int?)
(s/def ::initial-limit int?)
(s/def ::show-more-limit int?)
(s/def ::offset int?)
(s/def ::sort-dir keyword?)
(s/def ::ids-list (s/keys :req-un [::items ::loading? ::params]
                          :opt-un [::offset ::limit ::sort-dir ::initial-limit ::show-more-limit]))

(s/def :list/contract-invoices ::ids-list)
(s/def :list/job-proposals ::ids-list)
(s/def :list/job-feedbacks ::ids-list)
(s/def :list/job-invoices ::ids-list)
(s/def :list/job-sponsorships ::ids-list)
(s/def :list/user-sponsorships ::ids-list)
(s/def :list/employer-invoices-pending ::ids-list)
(s/def :list/employer-invoices-paid ::ids-list)
(s/def :list/freelancer-invoices-pending ::ids-list)
(s/def :list/freelancer-invoices-paid ::ids-list)
(s/def :list/search-freelancers ::ids-list)
(s/def :list/search-jobs ::ids-list)
(s/def :list/sponsorable-jobs ::ids-list)
(s/def :list/freelancer-feedbacks ::ids-list)
(s/def :list/employer-feedbacks ::ids-list)
(s/def :list/freelancer-invitations ::ids-list)
(s/def :list/freelancer-proposals ::ids-list)
(s/def :list/freelancer-contracts ::ids-list)
(s/def :list/freelancer-contracts-open ::ids-list)
(s/def :list/freelancer-contracts-done ::ids-list)
(s/def :list/freelancer-contracts-cancelled ::ids-list)
(s/def :list/employer-invitations ::ids-list)
(s/def :list/employer-proposals ::ids-list)
(s/def :list/employer-contracts ::ids-list)
(s/def :list/employer-contracts-open ::ids-list)
(s/def :list/employer-contracts-done ::ids-list)
(s/def :list/employer-contracts-cancelled ::ids-list)
(s/def :list/employer-jobs-open ::ids-list)
(s/def :list/employer-jobs-done ::ids-list)
(s/def :list/employer-jobs ::ids-list)
(s/def :list/freelancer-my-open-contracts ::ids-list)
(s/def :list/employer-jobs-open-select-field ::ids-list)

(s/def :search/category constants/categories)
(s/def :search/skills (s/coll-of pos?))
(s/def :search/payment-types (s/coll-of constants/payment-types))
(s/def :search/experience-levels (s/coll-of constants/experience-levels))
(s/def :search/estimated-durations (s/coll-of constants/estimated-durations))
(s/def :search/hours-per-weeks (s/coll-of constants/hours-per-weeks))
(s/def :search/min-budget (some-fn string? number?))
(s/def :search/min-budget-currency (partial contains? (set (keys constants/currencies))))
(s/def :search/min-employer-avg-rating u/rating?)
(s/def :search/min-employer-ratings-count u/rating?)
(s/def :search/country (partial >= (count constants/countries)))
(s/def :search/state (partial >= (count constants/united-states)))
(s/def :search/language (partial >= (count constants/languages)))
(s/def :search/offset int?)
(s/def :search/limit int?)
(s/def :search/min-avg-rating u/rating?)
(s/def :search/min-freelancer-ratings-count int?)
(s/def :search/min-hourly-rate (some-fn string? number?))
(s/def :search/max-hourly-rate (some-fn string? number?))
(s/def :search/hourly-rate-currency (partial contains? (set (keys constants/currencies))))

(s/def :form/search-jobs (s/keys))
(s/def :form/search-freelancers (s/keys))
(s/def ::gas-limit pos?)
(s/def ::errors (s/coll-of keyword?))
(s/def ::data (s/keys))
(s/def ::submit-form (s/keys :req-un [::loading? ::gas-limit]
                             :opt-un [::errors ::data]))

(s/def :form.invoice/pay-invoice ::submit-form)
(s/def :form.invoice/cancel-invoice ::submit-form)
(s/def :form.job/set-hiring-done ::submit-form)
(s/def :form.job/approve-sponsorable-job ::submit-form)
(s/def :form.job/add-job ::submit-form)
(s/def :form.contract/add-invitation ::submit-form)
(s/def :form.contract/add-proposal ::submit-form)
(s/def :form.contract/add-contract ::submit-form)
(s/def :form.contract/cancel-contract ::submit-form)
(s/def :form.contract/add-feedback ::submit-form)
(s/def :form.invoice/add-invoice ::submit-form)
(s/def :form.config/add-skills ::submit-form)
(s/def :form.user/set-user ::submit-form)
(s/def :form.user/set-freelancer ::submit-form)
(s/def :form.user/set-employer ::submit-form)
(s/def :form.user/register-freelancer ::submit-form)
(s/def :form.user/register-employer ::submit-form)
(s/def :form.user2/set-user-notifications ::submit-form)
(s/def :form.sponsor/add-job-sponsorship ::submit-form)
(s/def :form.config/set-configs ::submit-form)
(s/def :form.config/block-skills ::submit-form)
(s/def :form.config/set-skill-name ::submit-form)

(s/def :form.invoice/add-invoice-localstorage (s/map-of pos? (s/map-of keyword? any?)))

(s/def ::db (s/keys :req-un [::load-node-addresses? ::node-url ::web3 ::active-page ::provides-web3? ::contracts-not-found?
                             ::drawer-open? ::search-freelancers-filter-open?
                             ::search-jobs-filter-open? ::selected-currency ::snackbar ::my-addresses ::active-address
                             ::my-users-loaded? ::conversion-rates ::conversion-rates-historical
                             ::skill-load-limit ::active-setters? ::last-transaction-gas-used ::skills-loaded?
                             ::load-all-conversion-rates-interval ::dialog ::legacy-user-ids]))

(def generate-mode? false)

(def default-db
  {:web3 nil
   :web3-read-only nil
   :load-node-addresses? false
   :node-url "https://mainnet.infura.io/v3/fc19514210574bf580fbc13f58f65819"
   :active-page (u/match-current-location)
   :provides-web3? false
   :contracts-not-found? false
   :window/width-size (u/get-window-width-size js/window.innerWidth)
   :drawer-open? false
   :search-freelancers-filter-open? false
   :search-freelancers-skills-open? false
   :search-jobs-filter-open? false
   :search-jobs-skills-open? false
   :selected-currency 0
   :last-transaction-gas-used nil
   :snackbar {:open? false
              :message ""
              :auto-hide-duration 5000
              :on-request-close #(dispatch [:snackbar/close])}
   :dialog {:open? false
            :modal false
            :title ""
            :actions []
            :body ""
            :on-request-close #(dispatch [:dialog/close])}
   :eth/config {:max-user-languages 10
                :min-user-languages 1
                :max-freelancer-categories (dec (count constants/categories))
                :min-freelancer-categories 1
                :max-freelancer-skills 10
                :min-freelancer-skills 1
                :max-job-skills 7
                :min-job-skills 1
                :max-user-description 1000
                :max-job-description 1500
                :min-job-description 100
                :max-invoice-description 1000
                :max-feedback 1500
                :min-feedback 50
                :max-job-title 100
                :min-job-title 10
                :max-user-name 40
                :min-user-name 5
                :max-freelancer-job-title 50
                :min-freelancer-job-title 4
                :max-contract-desc 1500
                :max-proposal-desc 1500
                :max-invitation-desc 1500
                :max-message-length 1500
                :max-skills-create-at-once 4
                :adding-skills-enabled? 0
                :max-sponsor-name 50
                :max-sponsor-link 255
                :min-sponsorship-amount (js/parseInt (web3/to-wei 0.02 :ether))
                :max-job-allowed-users 10
                :min-job-allowed-users 1
                :max-gas-limit u/max-gas-limit}
   :active-setters? true
   :eth/contracts {:ethlance-user {:name "EthlanceUser" :setter? true :address "0x27d233fa6032e848a016092d70493b2a5f13a95f"}
                   :ethlance-user2 {:name "EthlanceUser2" :setter? true :address "0x42c3e6bf6e47ad3d6cbb0b966c44e9331e96dd3e"}
                   :ethlance-job {:name "EthlanceJob" :setter? true :address "0xB9E80ce5A7CbbA0Aab685797F6585AD1f3c90028"}
                   :ethlance-contract {:name "EthlanceContract" :setter? true :address "0x8F24AF20ad202C77686B771AD3dBc6b1fe28dDdD"}
                   :ethlance-feedback {:name "EthlanceFeedback" :setter? true :address "0x2249713725c8a4a070a61de0bdce6b1081014185"}
                   :ethlance-invoice {:name "EthlanceInvoice" :setter? true :address "0x78f1072964d7f110e06670c229794afbdce7e474"}
                   :ethlance-message {:name "EthlanceMessage" :setter? true :address "0xf94aa98bde7589719f1f08c6fb032debd0d7e9e6"}
                   :ethlance-config {:name "EthlanceConfig" :setter? true :address "0xe7d8d05f8328ea5b8fba5a77d4e4172487264bda"}
                   :ethlance-sponsor {:name "EthlanceSponsor" :setter? true :address "0xb9f7d3b60ec29bd73fd66428f140ed5b0e1ef6ec"}
                   :ethlance-sponsor-wallet {:name "EthlanceSponsorWallet" :address "0xc80d2cb06ce606395178692de07ea9da1f873aa3"}
                   :ethlance-db {:name "EthlanceDB" :address "0x5371a8d8d8a86c76de935821ad1a3e9b908cfced"}
                   :ethlance-views {:name "EthlanceViews" :address "0x1f286cB2EB7AE530FD85FD6EcE2e17d4f60D8DaA"}
                   :ethlance-search-freelancers {:name "EthlanceSearchFreelancers" :address "0x43386ad7af76ca5384bc06ae0c74e230f32744ee"}
                   :ethlance-search-jobs {:name "EthlanceSearchJobs" :address "0x9e2f85eea233047e527039681ad84448c8926690"}}
   :my-addresses []
   :my-addresses-forced []
   :active-address nil
   :active-user-events nil
   :my-users-loaded? false
   :blockchain/connection-error? false
   :conversion-rates {}
   :conversion-rates-historical {}
   :app/users {}
   :app/jobs {}
   :app/jobs.allowed-users {}
   :app/contracts {}
   :app/invoices {}
   :skills-loaded? false
   :app/skills {}
   :app/skill-count 0
   :app/sponsorships {}
   :app/messages {}
   :skill-load-limit 30
   :load-all-conversion-rates-interval nil
   :legacy-user-ids {}

   :list/contract-invoices {:items [] :loading? true :params {} :offset 0 :limit constants/list-limit :sort-dir :desc}
   :list/job-proposals {:items [] :loading? true :params {} :offset 0 :limit constants/list-limit :sort-dir :asc}
   :list/job-sponsorships {:items [] :loading? true :params {} :offset 0 :limit constants/list-limit}
   :list/user-sponsorships {:items [] :loading? true :params {} :offset 0 :limit constants/list-limit :sort-dir :desc}
   :list/job-feedbacks {:items [] :loading? true :params {} :offset 0 :initial-limit 1 :limit 1 :show-more-limit 8 :sort-dir :desc}
   :list/job-invoices {:items [] :loading? true :params {} :offset 0 :limit constants/list-limit :sort-dir :desc}
   :list/employer-invoices-pending {:items [] :loading? true :params {} :offset 0 :limit constants/list-limit :sort-dir :desc}
   :list/employer-invoices-paid {:items [] :loading? true :params {} :offset 0 :limit constants/list-limit :sort-dir :desc}
   :list/freelancer-invoices-pending {:items [] :loading? true :params {} :offset 0 :limit constants/list-limit :sort-dir :desc}
   :list/freelancer-invoices-paid {:items [] :loading? true :params {} :offset 0 :limit constants/list-limit :sort-dir :desc}
   :list/search-freelancers {:items [] :loading? true :params {} :offset 0 :limit 10}
   :list/search-jobs {:items [] :loading? true :params {} :offset 0 :limit 10}
   :list/sponsorable-jobs {:items [] :loading? true :params {} :offset 0 :limit 10}
   :list/freelancer-feedbacks {:items [] :loading? true :params {} :offset 0 :initial-limit 1 :limit 1 :show-more-limit 8 :sort-dir :desc}
   :list/employer-feedbacks {:items [] :loading? true :params {} :offset 0 :initial-limit 1 :limit 1 :show-more-limit 8 :sort-dir :desc}
   :list/freelancer-invitations {:items [] :loading? true :params {} :offset 0 :limit constants/list-limit :sort-dir :desc}
   :list/freelancer-proposals {:items [] :loading? true :params {} :offset 0 :limit constants/list-limit :sort-dir :desc}
   :list/freelancer-contracts {:items [] :loading? true :params {} :offset 0 :limit constants/list-limit :sort-dir :desc}
   :list/freelancer-contracts-open {:items [] :loading? true :params {} :offset 0 :limit constants/list-limit :sort-dir :desc}
   :list/freelancer-contracts-done {:items [] :loading? true :params {} :offset 0 :limit constants/list-limit :sort-dir :desc}
   :list/freelancer-contracts-cancelled {:items [] :loading? true :params {} :offset 0 :limit constants/list-limit :sort-dir :desc}
   :list/employer-invitations {:items [] :loading? true :params {} :offset 0 :limit constants/list-limit :sort-dir :desc}
   :list/employer-proposals {:items [] :loading? true :params {} :offset 0 :limit constants/list-limit :sort-dir :desc}
   :list/employer-contracts {:items [] :loading? true :params {} :offset 0 :limit constants/list-limit :sort-dir :desc}
   :list/employer-contracts-open {:items [] :loading? true :params {} :offset 0 :limit constants/list-limit :sort-dir :desc}
   :list/employer-contracts-done {:items [] :loading? true :params {} :offset 0 :limit constants/list-limit :sort-dir :desc}
   :list/employer-contracts-cancelled {:items [] :loading? true :params {} :offset 0 :limit constants/list-limit :sort-dir :desc}
   :list/employer-jobs-open {:items [] :loading? true :params {} :offset 0 :limit constants/list-limit :sort-dir :desc}
   :list/employer-jobs-done {:items [] :loading? true :params {} :offset 0 :limit constants/list-limit :sort-dir :desc}
   :list/employer-jobs {:items [] :loading? true :params {} :offset 0 :limit constants/list-limit :sort-dir :desc}
   :list/freelancer-my-open-contracts {:items [] :loading? true :params {}}
   :list/employer-jobs-open-select-field {:items [] :loading? false :params {}}

   :form.invoice/pay-invoice {:loading? false :gas-limit 250000}
   :form.invoice/cancel-invoice {:loading? false :gas-limit 150000}
   :form.job/set-hiring-done {:loading? false :gas-limit 120000}
   :form.job/approve-sponsorable-job {:loading? false :gas-limit 120000}
   :form.job/add-job {:loading? false
                      :gas-limit 2000000
                      :budget-enabled? false
                      :data {:job/id 0
                             :job/title ""
                             :job/description ""
                             :job/skills []
                             :job/language 40
                             :job/budget 0
                             :job/category 0
                             :job/payment-type 1
                             :job/experience-level 1
                             :job/estimated-duration 1
                             :job/hours-per-week 1
                             :job/freelancers-needed 1
                             :job/reference-currency 0
                             :job/sponsorable? false
                             :job/invitation-only? false
                             :job/allowed-users []}
                      :errors #{:job/title :job/description :job/skills :job/category :job/allowed-users}}

   :form.job/set-job {:loading? false
                      :gas-limit 2000000
                      :data {}
                      :errors #{}}

   :form.contract/add-invitation {:loading? false
                                  :gas-limit 550000
                                  :data {:invitation/description ""
                                         :contract/job 0}
                                  :errors #{:contract/job}}

   :form.contract/add-proposal {:loading? false
                                :gas-limit 550000
                                :data {:proposal/description ""
                                       :proposal/rate 0}
                                :errors #{}}

   :form.contract/add-contract {:loading? false
                                :gas-limit 200000
                                :data {:contract/description ""
                                       :contract/hiring-done? false}
                                :errors #{}}

   :form.contract/cancel-contract {:loading? false
                                   :gas-limit 200000
                                   :data {:contract/cancel-description ""}
                                   :errors #{}}

   :form.contract/add-feedback {:loading? false
                                :gas-limit 550000
                                :data {:contract/feedback ""
                                       :contract/feedback-rating 100}
                                :errors #{:contract/feedback}}

   :form.invoice/add-invoice {:loading? false
                              :gas-limit 600000
                              :data {:invoice/contract 0
                                     :invoice/description ""
                                     :invoice/conversion-rate 0
                                     :invoice/amount 0
                                     :invoice/rate 0
                                     :invoice/worked-hours 0
                                     :invoice/worked-minutes 0
                                     :invoice/worked-from (u/week-ago)
                                     :invoice/worked-to (t/today-at-midnight)}
                              :errors #{:invoice/contract}}

   :form.invoice/add-invoice-localstorage {}

   :form.sponsor/add-job-sponsorship {:loading? false
                                      :gas-limit 600000
                                      :data {:sponsorship/name ""
                                             :sponsorship/link ""
                                             :sponsorship/amount 1
                                             :sponsorship/currency 0}
                                      :errors #{}}

   :form.sponsor/refund-job-sponsorships {:loading? false :gas-limit u/max-gas-limit}

   :form.config/add-skills {:loading? false
                            :gas-limit 600000
                            :data {:skill/names []}
                            :errors #{:skill/names}}

   :form.config/set-configs {:loading? false :gas-limit 2000000}
   :form.config/block-skills {:loading? false :gas-limit u/max-gas-limit}
   :form.config/set-skill-name {:loading? false :gas-limit u/max-gas-limit}

   :form.user/set-user {:loading? false
                        :gas-limit 500000
                        :data {}
                        :errors #{}}

   :form.user/set-freelancer {:loading? false
                              :gas-limit 2000000
                              :data {}
                              :errors #{}
                              :open? false}

   :form.user/set-employer {:loading? false
                            :gas-limit 370000
                            :data {}
                            :errors #{}
                            :open? false}

   :form.user2/set-user-notifications {:loading? false
                                       :gas-limit 400000
                                       :data {}
                                       :errors #{}}

   :form.user/register-freelancer {:loading? false
                                   :gas-limit 3000000
                                   :open? true
                                   :data {:user/name ""
                                          :user/email ""
                                          :user/gravatar ""
                                          :user/country 0
                                          :user/languages [40]
                                          :user/github ""
                                          :user/linkedin ""
                                          :freelancer/available? true
                                          :freelancer/job-title ""
                                          :freelancer/hourly-rate 1
                                          :freelancer/hourly-rate-currency 0
                                          :freelancer/categories []
                                          :freelancer/skills []
                                          :freelancer/description ""}
                                   :errors #{:user/name :user/country :freelancer/job-title
                                             :freelancer/categories :freelancer/skills}}

   :form.user/register-employer {:loading? false
                                 :gas-limit 2000000
                                 :open? true
                                 :data {:user/name ""
                                        :user/email ""
                                        :user/gravatar ""
                                        :user/country 0
                                        :user/languages [40]
                                        :user/github ""
                                        :user/linkedin ""
                                        :employer/description ""}
                                 :errors #{:user/name :user/country}}

   :form.message/add-job-contract-message {:loading? false
                                           :gas-limit 500000
                                           :data {:message/text ""}
                                           :errors #{}}

   :form/search-jobs {:search/category 1
                      :search/skills []
                      :search/skills-or []
                      :search/payment-types [1 2 3]
                      :search/experience-levels [1 2 3]
                      :search/estimated-durations [1 2 3 4]
                      :search/hours-per-weeks [1 2]
                      :search/min-budget 0
                      :search/min-budget-currency 0
                      :search/min-employer-avg-rating 0
                      :search/min-employer-ratings-count 0
                      :search/country 0
                      :search/state 0
                      :search/language 0
                      :search/min-created-on 0
                      :search/offset 0
                      :search/limit 10}

   :form/search-freelancers {:search/category 2
                             :search/skills []
                             :search/skills-or []
                             :search/min-avg-rating 0
                             :search/min-freelancer-ratings-count 0
                             :search/min-hourly-rate 0
                             :search/max-hourly-rate 0
                             :search/hourly-rate-currency 0
                             :search/country 0
                             :search/state 0
                             :search/language 0
                             :search/job-recommendations 0
                             :search/offset 0
                             :search/limit 10}})
