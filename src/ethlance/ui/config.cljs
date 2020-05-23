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

   :graphql {:schema graphql.schema/schema
             ;; :url "http://localhost:4000/graphql"
             :url "http://192.168.0.111:4000/graphql"
             :jwt-sign-secret "SECRET"}})


(def development-config
  (-> general-config
      (assoc-in [:logging :level] :debug)
      (assoc-in [:router :routes] ethlance.shared.routes/dev-routes)))


;; TODO: generate based on whether dev, prod, qa
(defn get-config []
  development-config)
