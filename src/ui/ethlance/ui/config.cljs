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
    :scroll-top? true}})


;; TODO: generate based on whether dev, prod, qa
(defn get-config []
  general-config)
