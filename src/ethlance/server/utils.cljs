(ns ethlance.server.utils
  (:require [cljs-ipfs-api.files :as ipfs-files]
            [taoensso.timbre :as log]))

(defn- parse-meta [{:keys [:content :on-success :on-error]}]
  (try
    (-> (re-find #".+?(\{.+\})" content)
        second
        js/JSON.parse
        (js->clj :keywordize-keys true)
        on-success)
    (catch :default e
      (on-error e))))

(defn get-ipfs-meta [conn meta-hash]
  (js/Promise.
   (fn [resolve reject]
     (log/info (str "Downloading: " "/ipfs/" meta-hash) ::get-ipfs-meta)
     (ipfs-files/fget (str "/ipfs/" meta-hash)
                      {:req-opts {:compress false}}
                      (fn [err content]
                        (cond
                          err
                          (let [err-txt "Error when retrieving metadata from ipfs"]
                            (log/error err-txt (merge {:meta-hash meta-hash
                                                       :connection conn
                                                       :error err})
                                       ::get-ipfs-meta)
                            (reject (str err-txt " : " err)))

                          (empty? content)
                          (let [err-txt "Empty ipfs content"]
                            (log/error err-txt {:meta-hash meta-hash
                                                :connection conn} ::get-ipfs-meta)
                            (reject err-txt))

                          :else (parse-meta {:content content
                                             :on-success resolve
                                             :on-error reject})))))))
