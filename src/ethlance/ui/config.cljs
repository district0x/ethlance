(ns ethlance.ui.config
  (:require

   ;; District UI Components
   [district.ui.component.router]

   [ethlance.shared.graphql.schema :as graphql.schema]
   [ethlance.shared.routes]))


(def general-config
  {:logging
   {:level :info
    :console? true}

   :reagent-render
   {:id "app"
    :component-var #'district.ui.component.router/router}

   :router
   {:routes ethlance.shared.routes/routes
    :default-route :route/home
    :scroll-top? true
    :html5? true}

   :ipfs
   {:endpoint "/api/v0"
    :host "http://127.0.0.1:5001"
    :gateway "http://127.0.0.1:8080/ipfs"}

   :graphql
   {:schema graphql.schema/schema
    :url "http://localhost:6300/graphql"
    :jwt-sign-secret "SECRET"}

   :root-url "http://127.0.0.1:6500"

   :github
   {:client-id "83e6a6043ca4ae50f8b0"}

   })


(def development-config
  (-> general-config
      (assoc-in [:logging :level] :debug)
      (assoc-in [:router :routes] ethlance.shared.routes/dev-routes)))


;; TODO: generate based on whether dev, prod, qa
(defn get-config []
  development-config)
