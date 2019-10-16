(ns ethlance.server.graphql.mutations.sign-in
  (:require [cljs.nodejs :as nodejs]
            [taoensso.timbre :as log]
            [clojure.string :as str]
            [ethlance.ui.config :as config]))

(def json-web-token (nodejs/require "jsonwebtoken"))
(def eth-sig-util (nodejs/require "eth-sig-util"))

(defn sign-in
  "Graphql sign in mutation. Given `data` and `data-signature`
  recovers user address.
  If successful returns a jason web token containing the user address.
  Throws a Error otherwise."
  [_ {:keys [data-signature data] :as args}]
  (try
    (let [sign-secret (get-in (config/get-config) [:graphql :jwt-sign-secret])
          user-address (.recoverPersonalSignature eth-sig-util #js {:data data :sig data-signature})
          jwt (.sign json-web-token #js {:userAddress user-address} sign-secret)]
      jwt)
    (catch js/Error e
      (throw (js/Error. "Invalid signature")))))

(defn session-middleware
  "A middleware that checks the authorization header jason web token
  and adds currentUser (the user address) to the request if it validates."
  [req res next]
  (try
    (let [sign-secret (get-in (config/get-config) [:graphql :jwt-sign-secret])
          [_ jwt] (some->> req
                           .-headers
                           .-authorization
                           (re-matches #"Bearer (.+)"))]
      (when jwt
        (let [user-address (:userAddress (js->clj (.verify json-web-token jwt sign-secret) :keywordize-keys true))]
             (set! (.. req -currentUser) user-address))))
    (catch js/Error e
      (log/error "Unauthorized request" e)))
  (next))

;; For testing
;; http://localhost:6200/graphql

;; mutation{
;;   signIn(dataSignature:"0xfed02f1045f42eebdeea9f63096387076b180ed8b32aaa39f994058023b55d6c4293bc25ffc2df58f839d2c067157f09bda04911e961485dfdae08b6361114911c",
;;          data:"0x48692074686572652120596f7572207370656369616c206e6f6e63653a2037343566366630382d613537362d343137632d393461632d373764666233363034353366")
;; }
