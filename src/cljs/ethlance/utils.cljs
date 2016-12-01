(ns ethlance.utils
  (:require
    [bidi.bidi :as bidi]
    [cljs-react-material-ui.reagent :as ui]
    [cljs-time.coerce :refer [to-date-time to-long to-local-date-time]]
    [cljs-time.core :as t :refer [date-time to-default-time-zone]]
    [cljs-time.format :as time-format]
    [clojure.core.async :refer [chan <! >!]]
    [clojure.string :as string]
    [ethlance.routes :refer [routes]]
    [ethlance.styles :as styles]
    [medley.core :as medley])
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

(defn empty-job? [job]
  (zero? (:job/created-on job)))

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

(defn create-data-source [m val-key]
  (map (fn [[k v]] {"text" (get v val-key) "value" k}) (into [] m)))

(def data-source-config {"text" "text" "value" "value"})

(defn data-source-values [values]
  (set (map :value (js->clj values :keywordize-keys true))))

(defn anchor
  ([body route route-params]
   (anchor {} body route route-params))
  ([props body route route-params]
   [:a (merge
         {:style {:color (:primary1-color styles/palette)}
          :href (when-not (some nil? (vals route-params))
                  (medley/mapply path-for route route-params))
          :on-click #(.stopPropagation %)}
         props) body]))

(defn assoc-key-as-value [key-name m]
  (into {} (map (fn [[k v]]
                  {k (assoc v key-name k)}) m)))

(defn big-num->date-time [big-num]
  (to-default-time-zone (to-date-time (* (.toNumber big-num) 1000))))

(defn format-date [date]
  (time-format/unparse-local (time-format/formatters :rfc822) date))

(defn time-ago [time]
  (let [units [{:name "second" :limit 60 :in-second 1}
               {:name "minute" :limit 3600 :in-second 60}
               {:name "hour" :limit 86400 :in-second 3600}
               {:name "day" :limit 604800 :in-second 86400}
               {:name "week" :limit 2629743 :in-second 604800}
               {:name "month" :limit 31556926 :in-second 2629743}
               {:name "year" :limit nil :in-second 31556926}]
        diff (t/in-seconds (t/interval time (t/now)))]
    (if (< diff 5)
      "just now"
      (let [unit (first (drop-while #(or (>= diff (:limit %))
                                         (not (:limit %)))
                                    units))]
        (-> (/ diff (:in-second unit))
          js/Math.floor
          int
          (#(str % " " (:name unit) (when (> % 1) "s") " ago")))))))

(defn parse-props-children [props children]
  (if (map? props)
    [props children]
    [nil (concat [props] children)]))