(ns ethlance.ui.page.sign-up.subscriptions
  (:require
    [ethlance.ui.page.sign-up.events :as sign-up.events]
    [ethlance.ui.subscriptions :as ethlance-subs]
    [ethlance.ui.util.urls :as util.urls]
    [re-frame.core :as re]))


(re/reg-sub
  :page.sign-up/user-profile-image
  (fn [db]
    (get-in db [sign-up.events/state-key :user/profile-image])))

(re/reg-sub
  :page.sign-up/update-user-profile-image
  (fn [query-v]
    [(re/subscribe [:page.sign-up/user-profile-image])
     (re/subscribe [::ethlance-subs/active-user])])
  (fn [[image-from-form active-user] query]
    (util.urls/ipfs-hash->gateway-url (or image-from-form (:user/profile-image active-user)))))

(re/reg-sub
  :page.sign-up/form
  (fn [db]
    (get db sign-up.events/state-key)))
