(ns ethlance.ui.page.sign-up.subscriptions
  (:require
   [re-frame.core :as re]
   [ethlance.ui.page.sign-up.events :as sign-up.events]
   [ethlance.ui.subscription.utils :as subscription.utils]))

(def create-get-handler #(subscription.utils/create-get-handler sign-up.events/state-key %))

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
