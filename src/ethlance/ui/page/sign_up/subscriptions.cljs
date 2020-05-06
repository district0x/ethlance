(ns ethlance.ui.page.sign-up.subscriptions
  (:require
   [re-frame.core :as re]

   [ethlance.ui.page.sign-up.events :as sign-up.events]
   [ethlance.ui.subscription.utils :as subscription.utils]))


(def create-get-handler #(subscription.utils/create-get-handler sign-up.events/state-key %))


;;
;; Registered Subscriptions
;;

(re/reg-sub :page.sign-up/candidate-full-name (create-get-handler :candidate/full-name))
(re/reg-sub :page.sign-up/candidate-professional-title (create-get-handler :candidate/professional-title))
(re/reg-sub :page.sign-up/candidate-email (create-get-handler :candidate/email))
(re/reg-sub :page.sign-up/candidate-hourly-rate (create-get-handler :candidate/hourly-rate))
(re/reg-sub :page.sign-up/candidate-github-key (create-get-handler :candidate/github-key))
(re/reg-sub :page.sign-up/candidate-linkedin-key (create-get-handler :candidate/linkedin-key))
(re/reg-sub :page.sign-up/candidate-languages (create-get-handler :candidate/languages))
(re/reg-sub :page.sign-up/candidate-categories (create-get-handler :candidate/categories))
(re/reg-sub :page.sign-up/candidate-skills (create-get-handler :candidate/skills))
(re/reg-sub :page.sign-up/candidate-biography (create-get-handler :candidate/biography))
(re/reg-sub :page.sign-up/candidate-country (create-get-handler :candidate/country))
(re/reg-sub :page.sign-up/candidate-ready-for-hire? (create-get-handler :candidate/ready-for-hire?))

(re/reg-sub :page.sign-up/employer-full-name (create-get-handler :employer/full-name))
(re/reg-sub :page.sign-up/employer-professional-title (create-get-handler :employer/professional-title))
(re/reg-sub :page.sign-up/employer-email (create-get-handler :employer/email))
(re/reg-sub :page.sign-up/employer-github-key (create-get-handler :employer/github-key))
(re/reg-sub :page.sign-up/employer-linkedin-key (create-get-handler :employer/linkedin-key))
(re/reg-sub :page.sign-up/employer-languages (create-get-handler :employer/languages))
(re/reg-sub :page.sign-up/employer-biography (create-get-handler :employer/biography))
(re/reg-sub :page.sign-up/employer-country (create-get-handler :employer/country))

(re/reg-sub :page.sign-up/arbiter-full-name (create-get-handler :arbiter/full-name))
(re/reg-sub :page.sign-up/arbiter-professional-title (create-get-handler :arbiter/professional-title))
(re/reg-sub :page.sign-up/arbiter-fixed-rate-per-dispute (create-get-handler :arbiter/fixed-rate-per-dispute))
(re/reg-sub :page.sign-up/arbiter-email (create-get-handler :arbiter/email))
(re/reg-sub :page.sign-up/arbiter-github-key (create-get-handler :arbiter/github-key))
(re/reg-sub :page.sign-up/arbiter-linkedin-key (create-get-handler :arbiter/linkedin-key))
(re/reg-sub :page.sign-up/arbiter-languages (create-get-handler :arbiter/languages))
(re/reg-sub :page.sign-up/arbiter-biography (create-get-handler :arbiter/biography))
(re/reg-sub :page.sign-up/arbiter-country (create-get-handler :arbiter/country))
