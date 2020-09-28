(ns cljsjs.sentry-browser
  (:require ["@sentry/browser" :as sentry]))

(js/goog.exportSymbol "Sentry" sentry)
