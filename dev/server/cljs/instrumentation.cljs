(ns cljs.instrumentation
  "Placing spec instrumentation in a separate file to avoid private
  function warnings."
  (:require
   [taoensso.timbre :as log]
   [expound.alpha :as expound]
   [clojure.spec.alpha :as s]
   [orchestra-cljs.spec.test :as st]))


;; Better Spec Error Messages by default
(set! s/*explain-out* expound/printer)


(defn enable! []
  (log/debug "Enabling Instrumentation!")
  (st/instrument))


(defn disable! []
  (log/debug "Disabling Instrumentation!")
  (st/unstrument))
