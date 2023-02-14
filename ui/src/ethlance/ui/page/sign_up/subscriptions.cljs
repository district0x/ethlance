(ns ethlance.ui.page.sign-up.subscriptions
  (:require
    [ethlance.ui.page.sign-up.events :as sign-up.events]
    [re-frame.core :as re]))

(defn ipfs-hash->gateway-url
 [ipfs-hash]
 ; This URL should come from configuration
 (str "http://bafybeiegfw3b7s6zfcw27z5agsj3d7p2jaurohzgvjjyexzbbuwt5tdu7a.ipfs.localhost:8080/?filename=" ipfs-hash))

(re/reg-sub
  :page.sign-up/user-profile-image
  (fn [db]
    (ipfs-hash->gateway-url (get-in db [sign-up.events/state-key :user/profile-image]))))

(re/reg-sub
  :page.sign-up/form
  (fn [db]
    (get db sign-up.events/state-key)))
