(ns district.ui.router.events
  (:require
   [re-frame.core :refer [reg-event-fx trim-v]]

   ;; Effects
   [day8.re-frame.async-flow-fx]
   [district0x.re-frame.window-fx]
   [district.ui.router.effects :as effects]

   ;; Local
   [district.ui.router.queries :as queries]))


(def interceptors [trim-v])

(reg-event-fx
  ::start
  interceptors
  (fn [{:keys [:db]} [{:keys [:bide-router :html5? :scroll-top?]}]]
    {:db (-> db
           (queries/assoc-bide-router bide-router)
           (queries/assoc-html5 html5?)
           (queries/assoc-scroll-top scroll-top?))}))


(reg-event-fx
  ::active-page-changed*
  interceptors
  (fn [{:keys [:db]} [name params query]]
    (if (queries/bide-router db)                            ;; Initial :on-navigate is fired before ::start
      {:dispatch [::active-page-changed name params query]}
      {:async-flow {:first-dispatch [::do-nothing*]
                    :rules [{:when :seen?
                             :events [::start]
                             :dispatch [::active-page-changed name params query]}]}})))


(reg-event-fx
  ::active-page-changed
  interceptors
  (fn [{:keys [:db]} [name params query]]
    {:db (queries/assoc-active-page db {:name name :params params :query query})}))


(reg-event-fx
  ::do-nothing*
  (constantly nil))


(reg-event-fx
  ::watch-active-page
  interceptors
  (fn [_ [watchers]]
    {::effects/watch-active-page watchers}))


(reg-event-fx
  ::unwatch-active-page
  interceptors
  (fn [_ [watchers]]
    {::effects/unwatch-active-page watchers}))


(reg-event-fx
  ::navigate
  interceptors
  (fn [{:keys [:db]} [name params query]]
    (cond-> {::effects/navigate [(queries/bide-router db) name params query]}
      (queries/scroll-top? db) (assoc :window/scroll-to [0 0]))))


(reg-event-fx
  ::replace
  interceptors
  (fn [{:keys [:db]} [name params query]]
    {::effects/replace [(queries/bide-router db) name params query]}))


(reg-event-fx
  ::stop
  interceptors
  (fn [{:keys [:db]}]
    {:db (queries/dissoc-router db)}))
