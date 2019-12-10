(ns ethlance.server.graphql.authorization
  (:require [taoensso.timbre :as log]))

(defn token->user [event {:keys [:secret] :as config}]
  (let [{:keys [:access-token] :as headers} (js->clj (aget event "req" "headers") :keywordize-keys true)]
    (log/debug "token->user"  headers)
    (cond
      (nil? access-token)
      (log/info "No access_token header present in request")

      ;; TODO : parse the JWT token
      (= secret access-token)
      {:user/address "1"}

      :else (log/info "Invalid access_token header"))))
