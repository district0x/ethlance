(shadow/repl :dev-ui)
(require '[re-frame.db :refer [app-db]])
(require '[re-frame.core :as re])

(in-ns 'ethlance.ui.event.sign-in)
(def jwt-token  "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyQWRkcmVzcyI6IjB4MDkzNWQyZWM2NTE0NDM0M2RmNjdiZDNjNzM5OWMzN2JlYjFiYmNlMCIsImlhdCI6MTY3NTA4ODQ1NH0.Xz4zWwWusCR9f-QEuVq7PWsIttwaZoYVZG1DmVGov0k")
(re/dispatch [:district.ui.graphql.events/set-authorization-token jwt-token])

(:active-session (akiroz.re-frame.storage/<-store :ethlance))

; Manual routing
(re/dispatch [:district.ui.router.events/navigate :route.job/jobs {} {}])
(re/dispatch [:district.ui.router.events/navigate :route.misc/about {} {}])
(re/dispatch [:district.ui.router.events/navigate :route.misc/about])

; Fill new-job form
; http://d0x-vm:6500/jobs/new
(defn fill-new-job-form [db & {:keys [] :as overrides}]
  (let [default-vals {:job/type :job
                      :job/title "Siimar"
                      :job/description "Test description"
                      :job/category "Admin Support"
                      :job/required-experience-level :intermediate
                      :job/bid-option :hourly-rate
                      :job/required-availability :part-time
                      :job/estimated-project-length :week
                      :job/required-skills #{"Translation English Spanish" "Sketch"}
                      :job/form-of-payment :ethereum
                      :job/with-arbiter? false}
        with-overrides (merge default-vals overrides)]
  (reset! db (merge @app-db {:page.new-job with-overrides}))))

(fill-new-job-form app-db :name "Toomas")

; Pretty-print clojure struct (edn serialization)
(defn write-edn [deps-map]
  (binding [*print-readably* true
            *print-namespace-maps* false]
    (with-out-str (cljs.pprint/pprint deps-map))))
