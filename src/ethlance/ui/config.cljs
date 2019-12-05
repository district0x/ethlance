(ns ethlance.ui.config
  (:require

   ;; District UI Components
   [district.ui.component.router]

   [ethlance.shared.routes]))


(def general-config
  {:logging
   {:level :debug
    :console? true}

   :reagent-render
   {:id "app"
    :component-var #'district.ui.component.router/router}

   :router
   {:routes ethlance.shared.routes/routes
    :default-route :route/home
    :scroll-top? true
    :html5? true}

   ;; Default one, should be moved to per env config file
   :graphql {:jwt-sign-secret "SECRET"}})


(def development-config
  (-> general-config
      (assoc-in [:router :routes] ethlance.shared.routes/dev-routes)))


;; TODO: generate based on whether dev, prod, qa
(defn get-config []
  development-config)
