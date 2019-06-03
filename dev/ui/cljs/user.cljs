(ns cljs.user
  (:require
   [mount.core :as mount]
   [ethlance.ui.core]))


(enable-console-print!)


(defn start []
  (ethlance.ui.core/init))


(defn stop []
  (mount/stop))


(defn restart []
  (stop)
  (start))
