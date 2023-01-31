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

(defn token->user [access-token sign-in-secret]
  (try-catch
   (cond
     (nil? access-token)
     (log/info "No access-token header present in request")

     access-token
     (let [user {:user/address (aget (parse-jwt access-token sign-in-secret) "userAddress")}]
       user)

     :else (log/info "Invalid access-token header"))))
