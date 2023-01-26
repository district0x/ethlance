(ns district.ui.router.queries
  (:require [bide.core :as bide])
  (:refer-clojure :exclude [resolve]))

(def db-key :district.ui.router)

(defn active-page [db]
  (-> db db-key :active-page))

(defn active-page-name [db]
  (:name (active-page db)))

(defn active-page-params [db]
  (:params (active-page db)))

(defn active-page-query [db]
  (:query (active-page db)))

(defn assoc-active-page [db active-page]
  (assoc-in db [db-key :active-page] active-page))

(defn bide-router [db]
  (-> db db-key :bide-router))

(defn html5? [db]
  (-> db db-key :html5?))

(defn scroll-top? [db]
  (-> db db-key :scroll-top?))

(defn resolve [db & args]
  (apply bide/resolve (bide-router db) args))

(defn match [db path]
  (bide/match (bide-router db) path))

(defn assoc-bide-router [db bide-router]
  (assoc-in db [db-key :bide-router] bide-router))

(defn assoc-html5 [db html5?]
  (assoc-in db [db-key :html5?] html5?))

(defn assoc-scroll-top [db scroll-top?]
  (assoc-in db [db-key :scroll-top?] scroll-top?))

(defn dissoc-router [db]
  (dissoc db db-key))
