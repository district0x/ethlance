(ns ethlance.utils
  (:require
    [bidi.bidi :as bidi]
    [cljs-react-material-ui.reagent :as ui]
    [clojure.core.async :refer [chan <! >!]]
    [clojure.string :as string]
    [ethlance.routes :refer [routes]]
    [ethlance.styles :as styles])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn path-for [& args]
  (str "#" (apply bidi/path-for routes args)))

(defn location-hash []
  (let [hash (-> js/document
               .-location
               .-hash
               (string/split #"\?")
               first
               (string/replace "#" ""))]
    (if (empty? hash) "/" hash)))

(defn nsname [x]
  (when x
    (str (when-let [n (namespace x)] (str n "/")) (name x))))

(defn match-current-location []
  (bidi/match-route routes (location-hash)))

(defn truncate
  "Truncate a string with suffix (ellipsis by default) if it is
   longer than specified length."
  ([string length]
   (truncate string length "..."))
  ([string length suffix]
   (let [string-len (count string)
         suffix-len (count suffix)]
     (if (<= string-len length)
       string
       (str (subs string 0 (- length suffix-len)) suffix)))))

(defn sha3 [& args]
  (apply js/SoliditySha3.sha3 (map #(if (keyword? %) (nsname %) %) args)))

(defn big-num->num [x]
  (if (aget x "toNumber")
    (.toNumber x)
    x))

(defn big-nums->nums [coll]
  (map big-num->num coll))

(defn ensure-vec [x]
  (if (sequential? x) x [x]))

(defn empty-user? [user]
  (zero? (:user/created-on user)))

(defn subheader [title]
  [ui/subheader {:style styles/subheader} title])

(defn debounce-ch
  ([c ms] (debounce-ch (chan) c ms false))
  ([c ms instant] (debounce-ch (chan) c ms instant))
  ([c' c ms instant]
   (go
     (loop [start (js/Date.) timeout nil]
       (let [loc (<! c)]
         (when timeout
           (js/clearTimeout timeout))
         (let [diff (- (js/Date.) start)
               delay (if (and instant
                              (or (>= diff ms)
                                  (not timeout)))
                       0 ms)
               t (js/setTimeout #(go (>! c' loc)) delay)]
           (recur (js/Date.) t)))))
   c'))

(defn debounce
  ([f ms] (debounce f ms false))
  ([f ms instant]
   (let [change-ch (chan)
         debounced-chan (debounce-ch change-ch ms instant)]
     (go-loop []
              (let [args (<! debounced-chan)]
                (apply f args)
                (recur)))
     (fn [& args]
       (go (>! change-ch args))))))

(defn create-data-source [m]
  (map (fn [[k v]] {"text" v "value" k}) (into [] m)))

(def data-source-config {"text" "text" "value" "value"})

(defn data-source-values [values]
  (set (map :value (js->clj values :keywordize-keys true))))