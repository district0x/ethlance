(ns ethlance.server.graphql.authorization
  (:require [cljs.nodejs :as nodejs]
            [taoensso.timbre :as log]
            [district.shared.error-handling :refer [try-catch]]))

(defonce JsonWebToken (nodejs/require "jsonwebtoken"))
(defonce EthSigUtil (nodejs/require "eth-sig-util"))

(defn recover-personal-signature [data data-signature]
  (js-invoke EthSigUtil "recoverPersonalSignature" #js {:data data :sig data-signature}))

(defn create-jwt [address secret]
  (js-invoke JsonWebToken "sign" #js {:userAddress address} secret))

(defn parse-jwt [token secret]
  (js-invoke JsonWebToken "verify" token secret))

(defn token->user [event {{:keys [:sign-in-secret]} :graphql}]
  (try-catch
   (let [{:keys [:access-token] :as headers} (js->clj (aget event "req" "headers") :keywordize-keys true)]
     (log/debug "token->user" headers)
     (cond
       (nil? access-token)
       (log/info "No access-token header present in request")

       access-token
       (let [user {:user/address (aget (parse-jwt access-token sign-in-secret) "userAddress")}]
         (log/debug "returning user" user)
         user)

       :else (log/info "Invalid access-token header")))))
