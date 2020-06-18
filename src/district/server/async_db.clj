(ns district.server.async-db)

(defmacro with-async-resolver-tx [conn & body]
  `(js/Promise.
    (fn [resolve# reject#]
      (.then (district.server.async-db/get-connection)
             (fn [~conn]
               (district.server.async-db/begin-tx ~conn)
               (cljs.core.async/take! (district.shared.async-helpers/safe-go ~@body)
                                      (fn [v-or-err#]

                                        (if (cljs.core/instance? js/Error v-or-err#)
                                          (do
                                            (district.server.async-db/rollback-tx ~conn)
                                            (district.server.async-db/release-connection ~conn)
                                            (reject# v-or-err#))

                                          (do
                                            (district.server.async-db/commit-tx ~conn)
                                            (district.server.async-db/release-connection ~conn)
                                            (resolve# v-or-err#))))))))))

(defmacro with-async-resolver-conn [conn & body]
  `(js/Promise.
    (fn [resolve# reject#]
      (.then (district.server.async-db/get-connection)
             (fn [~conn]
               (cljs.core.async/take! (district.shared.async-helpers/safe-go ~@body)
                                      (fn [v-or-err#]

                                        ;; here we have the result so it is safe to release the connection
                                        (district.server.async-db/release-connection ~conn)

                                        (if (cljs.core/instance? js/Error v-or-err#)
                                          (reject# v-or-err#)
                                          (resolve# v-or-err#)))))))))
