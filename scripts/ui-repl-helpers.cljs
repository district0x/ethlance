(shadow/repl :dev-ui)
(require '[re-frame.db :refer [app-db]])
(require '[re-frame.core :as re])

; Set on window object
(set! (.. js/window -zeFun) (fn [] (re/dispatch [:district.ui.router.events/navigate :route.user/profile nil {:tab :arbiter}])))

(in-ns 'ethlance.ui.event.sign-in)
(def jwt-token  "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyQWRkcmVzcyI6IjB4MDkzNWQyZWM2NTE0NDM0M2RmNjdiZDNjNzM5OWMzN2JlYjFiYmNlMCIsImlhdCI6MTY3NTA4ODQ1NH0.Xz4zWwWusCR9f-QEuVq7PWsIttwaZoYVZG1DmVGov0k")
(def jwt-token  "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyQWRkcmVzcyI6IjB4MDkzNWQyZWM2NTE0NDM0M2RmNjdiZDNjNzM5OWMzN2JlYjFiYmNlMCIsImlhdCI6MTY3ODQ5NTY5OX0.PS6pTF_q9JDe0fzVulLJRrutJ675IC_M9RJYLCDuJSs")
(re/dispatch [:district.ui.graphql.events/set-authorization-token jwt-token])
(re/dispatch [:district.ui.graphql.events/set-authorization-token (get-in @app-db [:active-session :jwt])])
(get-in @app-db [:active-session])
(:active-session (akiroz.re-frame.storage/<-store :ethlance))

; Read ethlance active session from local storage
(akiroz.re-frame.storage/<-store :ethlance)

; Remove ethlance active session data from local storage
(akiroz.re-frame.storage/->store :ethlance nil)

; Cause new sign-in
(re/dispatch [:user/sign-out])
(re/dispatch [:user/sign-in])

; Manual routing
(re/dispatch [:district.ui.router.events/navigate :route.job/jobs {} {}])
(re/dispatch [:district.ui.router.events/navigate :route.misc/about {} {}])
(re/dispatch [:district.ui.router.events/navigate :route.misc/about])
(re/dispatch [:district.ui.router.events/navigate :route.job/detail {:contract "0x739c79FEE46b13227fBa11f54126bbD7710CF2e5"}])
(re/dispatch [:district.ui.router.events/navigate :route.job/detail])

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

; namespaced keyword experiment
(in-ns 'is.mad.one)
(def model {::name "First"})
(in-ns 'is.mad.two)
(def model {::name "Second"})
(in-ns 'is.mad.three)


; Fill in new job form

(def state-default
  {:job/title "Rauamaak on meie saak"
   :job/description "Tee t88d ja n2e vaeva"
   :job/category "Admin Support"
   :job/bid-option :hourly-rate
   :job/required-experience-level :intermediate
   :job/estimated-project-length :day
   :job/required-availability :full-time
   :job/required-skills #{"Somali" "Solidity"}
   :job/token-type :eth
   :job/token-amount 2
   :job/token-address "0x1111111111111111111111111111111111111111"
   :job/token-id 0
   :job/with-arbiter? true})

(reset! app-db (assoc-in @app-db [:page.new-job] state-default))

; Querying GraphQL manually
(def query [:job-search {:search-params {:feedback-max-rating 3}}
            [:total-count
             [:items [:job/id]]]])

(re/dispatch [:district.ui.graphql.events/query {:query {:queries [query]}}])

; Debugging job contract events
(require '[re-frame.db :refer [app-db]])
(require '[re-frame.core :as re])
(re/dispatch [:page.job-contract/set-feedback-text "Wadiis"])
(get-in @app-db [:page.job-contract :feedback-text])

; Decoding :Invoice-created event data
(in-ns 'ethlance.ui.page.new-invoice.events)

(defn decode [web3 event-interface {:keys [:data :topics]}]
  (web3-eth/decode-log web3 (:inputs event-interface) data (drop 1 topics)))

(def r (decode (:web3 @invoice-data) (:interface @invoice-data) (get-in @invoice-data [:tx :events :0 :raw])))

(require '[camel-snake-kebab.core :as cs :include-macros true])
(require '[camel-snake-kebab.extras :refer [transform-keys]])
(defn obj->clj [obj]
  (js->clj (-> obj js/JSON.stringify js/JSON.parse)))

(defn remove-unnecessary-keys [k-v]
  (let [length-key "__length__"
        positional-arguments-count (int (get k-v length-key "0"))
        numeric-keys (map str (range positional-arguments-count))]
    (apply dissoc (into [k-v length-key] numeric-keys))))

(defn parse-event [web3 contract-instance raw-event event-name]
  (let [event-interface (web3-helpers/event-interface contract-instance event-name)
        event-data (:data raw-event)
        event-topics (:topics raw-event)
        decoded-event (web3-eth/decode-log web3 (:inputs event-interface) event-data (drop 1 event-topics))]
   (-> decoded-event
       obj->clj
       remove-unnecessary-keys
       web3-helpers/js->cljkk
       clojure.walk/keywordize-keys)))

(def raw-event (get-in @invoice-data [:tx :events :0 :raw]))
(def contract-instance (smart-contracts.queries/instance @app-db :ethlance))
(def web3 (web3-queries/web3 @app-db))

(parse-event web3 contract-instance raw-event :Invoice-created)
(def xx (parse-event web3 contract-instance raw-event :Invoice-created))

(apply dissoc (into [(obj->clj r)] (map str (range 10))))
(dissoc (obj->clj r :keywordize-keys true) :0 :1 :2 :3 :4)
