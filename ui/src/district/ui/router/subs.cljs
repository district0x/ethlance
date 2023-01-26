(ns district.ui.router.subs
  (:require
    [district.ui.router.queries :as queries]
    [re-frame.core :refer [reg-sub]]))

(defn- sub-fn [query-fn]
  (fn [db [_ & args]]
    (apply query-fn db args)))

(reg-sub
  ::active-page
  queries/active-page)

(reg-sub
  ::active-page-name
  queries/active-page-name)

(reg-sub
  ::active-page-params
  queries/active-page-params)

(reg-sub
  ::active-page-query
  queries/active-page-query)

(reg-sub
  ::resolve
  (sub-fn queries/resolve))

(reg-sub
  ::match
  (sub-fn queries/match))

(reg-sub
  ::bide-router
  queries/bide-router)

(reg-sub
  ::html5?
  queries/html5?)
