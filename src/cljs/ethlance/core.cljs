(ns ethlance.core
  (:require
    [bidi.bidi :as bidi]
    [cljs-time.extend]
    [cljs.spec :as s]
    [cljsjs.bignumber]
    [cljsjs.material-ui]
    [cljsjs.react-flexbox-grid]
    [cljsjs.web3]
    [cljsjs.react-truncate]
    [ethlance.components.main-panel :refer [main-panel]]
    [ethlance.events]
    [ethlance.routes :refer [routes]]
    [ethlance.subs]
    [ethlance.utils :as u]
    [goog.string.format]
    [madvas.re-frame.google-analytics-fx :as google-analytics-fx]
    [print.foo :include-macros true]
    [re-frame.core :refer [dispatch dispatch-sync clear-subscription-cache!]]
    [reagent.core :as reagent]))

(defn mount-root []
  (s/check-asserts goog.DEBUG)
  (google-analytics-fx/set-enabled! (not goog.DEBUG))
  (clear-subscription-cache!)
  ;(.clear js/console)
  (reagent/render [main-panel] (.getElementById js/document "app")))

(defn ^:export init []
  (dispatch-sync [:initialize])
  (set! (.-onhashchange js/window) #(dispatch [:set-active-page (u/match-current-location)]))
  (mount-root))