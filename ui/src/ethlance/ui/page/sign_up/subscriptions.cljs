(ns ethlance.ui.page.sign-up.subscriptions
  (:require
    [ethlance.ui.page.sign-up.events :as sign-up.events]
    [re-frame.core :as re]))


(re/reg-sub
  :page.sign-up/form
  (fn [db]
    (get db sign-up.events/state-key)))
