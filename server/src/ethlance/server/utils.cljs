(ns ethlance.server.utils
  (:require
    [cljs.reader]
    [cljs-ipfs-api.files :as ipfs-files]
    [clojure.string :as str]
    [taoensso.timbre :as log]))


(defn- parse-json
  [string]
  (js->clj (.parse js/JSON string) :keywordize-keys true))


(defn- parse-edn
  [string]
  (cljs.reader/read-string string))


(defn- parse-meta
  "Gracefully handles JSON or EDN data from IPFS"
  [{:keys [:content :on-success :on-error]}]
  (let [content (str/replace content "\n" "")
        between-curly-braces #".+?(\{.+\}).*"
        content-string (second (re-find between-curly-braces content))]
    (try
      (on-success (parse-json content-string))
      (catch :default e
        (try
          (on-success (parse-edn content-string))
          (catch :default e
            (on-error e)))))))


(defn get-ipfs-meta
  [conn meta-hash]
  (log/info (str ">>> 1 of 3: IPFS get-ipfs-meta " meta-hash))
  (js/Promise.
    (fn [resolve reject]
      (log/info (str "2 of 3: Downloading: " "/ipfs/" meta-hash) ::get-ipfs-meta)
      (ipfs-files/fget (str "/ipfs/" meta-hash)
                       {:req-opts {:compress false}}
                       (fn [err content]
                         (log/info (str "3 of 3: ethlance.server.utils/get-ipfs-meta IPFS callback" {:err err :content content}))
                         (cond
                           err
                           (let [err-txt "Error when retrieving metadata from ipfs"]
                             (log/error err-txt (merge {:meta-hash meta-hash
                                                        :connection conn
                                                        :error err})
                                        ::get-ipfs-meta)
                             (println ">>> get-ipfs-meta REJECT")
                             (reject (str err-txt " : " err)))

                           (empty? content)
                           (let [err-txt "Empty ipfs content"]
                             (log/error err-txt {:meta-hash meta-hash
                                                 :connection conn} ::get-ipfs-meta)
                             (reject err-txt))

                           :else (parse-meta {:content content
                                              :on-success resolve
                                              :on-error reject})))))))
