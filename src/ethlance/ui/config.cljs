(ns ethlance.ui.config
  (:require [district.ui.component.router :as router]
            [ethlance.shared.graphql.schema :refer [schema]]
            [ethlance.shared.routes :as routes]))

(def general-config
  {:logging
   {:level :info
    :console? true}
   :reagent-render
   {:id "app"
    :component-var #'router/router}
   :router
   {:routes routes/routes
    :default-route :route/home
    :scroll-top? true
    :html5? true}
   :ipfs
   {:endpoint "/api/v0"
    :host "http://127.0.0.1:5001"
    :gateway "http://127.0.0.1:8080/ipfs"}
   :graphql
   {:schema schema
    :url "http://localhost:6300/graphql"
    :jwt-sign-secret "SECRET"}
   :root-url "http://127.0.0.1:6500"
   :github
   {:client-id "83e6a6043ca4ae50f8b0"}
   :linkedin
   {:client-id "86csctqngadad5"}
   :conversion-rates {:from-currencies [:ETH]
                      :to-currencies [:USD]}
   })

(def development-config
  (-> general-config
      (assoc-in [:logging :level] :debug)
      (assoc-in [:router :routes] routes/dev-routes)))

;; TODO: generate based on whether dev, prod, qa
(defn get-config []
  development-config)
