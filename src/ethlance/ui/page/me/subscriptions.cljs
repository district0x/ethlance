(ns ethlance.ui.page.me.subscriptions
  (:require
   [re-frame.core :as re]
   [ethlance.ui.page.me.events :as me.events]))


(re/reg-sub
 :page.me/current-sidebar-choice
 (fn [db _]
   (-> db me.events/state-key :current-sidebar-choice)))

