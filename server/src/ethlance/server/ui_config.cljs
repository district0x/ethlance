(ns ethlance.server.ui-config
  (:require
    [district.shared.async-helpers :refer [<? safe-go]]
    [district.server.async-db :as db :include-macros true]))

(defn fetch-config []
  (db/with-async-resolver-conn conn
    (let [tokens-query {:select [:token-detail/id
                                 :token-detail/name
                                 :token-detail/symbol] :from [:TokenDetail]}
          token-details (<? (db/all conn tokens-query))]
      {:token-details token-details})))
