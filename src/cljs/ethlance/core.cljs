(ns ethlance.core
  (:require
    [cljs-time.extend]
    [cljs.spec.alpha :as s]
    [cljsjs.bignumber]
    [cljsjs.material-ui]
    [cljsjs.react-flexbox-grid]
    [cljsjs.web3]
    [cljsjs.react-truncate]
    [cljsjs.oauthio]
    [district.ui.mobile]
    [ethlance.components.main-panel :refer [main-panel]]
    [ethlance.events]
    [ethlance.routes :refer [routes]]
    [ethlance.subs]
    [ethlance.utils :as u]
    [goog.string.format]
    [madvas.re-frame.google-analytics-fx :as google-analytics-fx]
    [mount.core :as mount]
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
  (s/check-asserts goog.DEBUG)
  (google-analytics-fx/set-enabled! (not goog.DEBUG))
  (.initialize js/OAuth "F9gUx3gjNFE0DTO4k6WiiJv5pcA")
  (dispatch-sync [:initialize])
  (mount/start)
  (set! (.-onhashchange js/window) #(dispatch [:set-active-page (u/match-current-location)]))
  (mount-root))
