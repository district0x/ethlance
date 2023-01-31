(ns district.ui.router
  (:require
    [bide.core :as bide]
    [cljs.spec.alpha :as s]
    [clojure.string :as string]
    [district.ui.router.events :as events]
    [mount.core :as mount :refer [defstate]]
    [re-frame.core :refer [dispatch-sync dispatch]]))

(declare start)
(declare stop)
(defstate router
  :start (start (:router (mount/args)))
  :stop (stop))

(s/def ::default-route keyword?)
(s/def ::routes sequential?)
(s/def ::html5-hosts #(or (sequential? %) (string? %)))
(s/def ::opts (s/nilable (s/keys :req-un [::default-route ::routes]
                                 :opt-un [::html5-hosts])))

(defn- hostname-html5-host? [html5-hosts]
  (when html5-hosts
    (contains? (cond-> html5-hosts
                 (string? html5-hosts) (string/split ",")
                 true set)
               (aget js/window "location" "hostname"))))

(defn start [{:keys [:routes :html5? :default-route :html5-hosts] :as opts}]
  (s/assert ::opts opts)
  (let [router (bide/router routes)
        html5? (or html5? (hostname-html5-host? html5-hosts))
        opts (merge opts
                    {:bide-router router
                     :html5? html5?})]
    (bide/start! router {:html5? html5?
                         :default default-route
                         :on-navigate #(dispatch [::events/active-page-changed* %1 %2 %3])})
    (dispatch-sync [::events/start opts])
    opts))


(defn stop []
  (dispatch-sync [::events/stop]))
