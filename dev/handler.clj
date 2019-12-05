(ns handler
  "Custom Figwheel Request Handler"
  (:require
   [clojure.java.io :as io]
   [ring.middleware.resource :as ring-resource]))


(def figwheel-request-handler
  (ring-resource/wrap-resource
    (fn [& _]
      {:status 200
       :headers {"Content-Type" "text/html"}
       :body (-> "public/index.html" io/resource slurp)})
   "public"))
