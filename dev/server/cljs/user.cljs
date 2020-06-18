(ns cljs.user
  (:require
   [honeysql.core :as sql]
   [mount.core :as mount]
   [taoensso.timbre :as log]))

(def sql-format
  "Shorthand for honeysql.core/format"
  sql/format)


(def help-message "
  CLJS-Server Repl Commands:

  -- Development Lifecycle --
  /Nothing Here, Yet/

  -- Misc --
  (help)                          ;; Display this help message

")


(defn help
  "Display a help message on development commands."
  []
  (println help-message))


(defn -dev-main
  "Commandline Entry-point for node dev_server.js"
  [& args]
  (help))


(set! *main-cli-fn* -dev-main)
