(ns ethlance.utils
  (:require
    [bidi.bidi :as bidi]
    [camel-snake-kebab.core :as cs :include-macros true]
    [camel-snake-kebab.extras :refer [transform-keys]]
    [cljs-react-material-ui.icons :as icons]
    [cljs-react-material-ui.reagent :as ui]
    [cljs-time.coerce :refer [to-date-time to-long to-local-date-time]]
    [cljs-time.core :as t :refer [date-time to-default-time-zone]]
    [cljs-time.format :as time-format]
    [clojure.core.async :refer [chan <! >!]]
    [clojure.string :as string]
    [ethlance.constants :as constants]
    [ethlance.routes :refer [routes]]
    [ethlance.styles :as styles]
    [goog.string :as gstring]
    [goog.string.format]
    [medley.core :as medley]
    [reagent.core :as r]
    [cljs-web3.core :as web3])
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

(defn set-location-hash! [s]
  (set! (.-hash js/location) s))

(defn nav-to! [route route-params]
  (set-location-hash! (medley/mapply path-for route route-params)))

(defn ns+name [x]
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
  (apply js/SoliditySha3.sha3 (map #(if (keyword? %) (ns+name %) %) args)))

(defn big-num->num [x]
  (if (and x (aget x "toNumber"))
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

(defn empty-contract? [contract]
  (zero? (:contract/job contract)))

(defn empty-invoice? [invoice]
  (zero? (:invoice/created-on invoice)))

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

(defn assoc-key-as-value [key-name m]
  (into {} (map (fn [[k v]]
                  {k (assoc v key-name k)}) m)))

(defn big-num-pos? [x]
  (when x
    (.greaterThan x 0)))

(defn big-num->date-time [big-num]
  (when (big-num-pos? big-num)
    (to-default-time-zone (to-date-time (* (.toNumber big-num) 1000)))))

(defn format-date [date]
  (when date
    (time-format/unparse-local (time-format/formatters :rfc822) date)))

(defn time-ago [time]
  (when time
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
            (#(str % " " (:name unit) (when (> % 1) "s") " ago"))))))))

(defn parse-props-children [props children]
  (if (map? props)
    [props children]
    [nil (concat [props] children)]))

(defn round
  ([d] (round d 2))
  ([d precision]
   (let [factor (js/Math.pow 10 precision)]
     (/ (js/Math.round (* d factor)) factor))))

(defn eth [x]
  (str (round (web3/from-wei (if x (.toNumber x) 0) :ether)) " Ξ"))

(defn format-rate [rate payment-type]
  (when rate
    (str (eth rate)
         (when (= 1 payment-type)
           " / hr"))))

(defn pluralize [text count]
  (str text (when (not= count 1) "s")))

(defn country-name [country-id]
  (when (pos? country-id)
    (nth constants/countries (dec country-id))))

(defn rating->star [rating]
  (/ (or rating 0) 20))

(defn star->rating [star]
  (* (or star 0) 20))

(defn rand-str [n]
  (let [chars-between #(map char (range (.charCodeAt %1) (inc (.charCodeAt %2))))
        chars (concat (chars-between \0 \9)
                      (chars-between \a \z)
                      (chars-between \A \Z)
                      [\_])
        password (take n (repeatedly #(rand-nth chars)))]
    (reduce str password)))

(defn create-with-default-props [component default-props]
  (fn [props & children]
    (let [[props children] (parse-props-children props children)]
      (into [] (concat
                 [component (r/merge-props default-props props)]
                 children)))))

(defn gravatar-url [hash]
  (gstring/format "http://s.gravatar.com/avatar/%s?s=80" hash))

(defn list-filter-loaded [list non-empty-pred]
  (-> list
    (assoc :loading? (or (:loading? list) (some (complement non-empty-pred) (:items list))))
    (update :items (partial filter non-empty-pred))))

(defn paginate [offset limit coll]
  (->> coll
    (drop offset)
    (take limit)))


(defn set-default-props! [react-class default-props]
  (let [current-defaults (-> (aget react-class "defaultProps")
                           (js->clj :keywordize-keys true))
        new-props (merge current-defaults (transform-keys cs/->camelCase default-props))]
    (aset react-class "defaultProps" (clj->js new-props))))

(defn table-cell-clicked? [e]
  "Sometimes .stopPropagation doesn't work in material-ui tables"
  (instance? js/HTMLTableCellElement (aget e "target")))

(defn table-row-nav-to-fn [& args]
  (fn [e]
    (when (table-cell-clicked? e)
      (apply nav-to! args))))

(def first-word
  (memoize (fn [x]
             (first (string/split x #" ")))))

(defn sort-by-desc [key-fn coll]
  (sort-by key-fn #(compare %2 %1) coll))

(defn sort-desc [coll]
  (sort #(compare %2 %1) coll))

(defn sort-in-dir [dir coll]
  (if (= dir :desc)
    (sort-desc coll)
    (sort coll)))

(defn sort-paginate-ids [{:keys [offset limit sort-dir]} ids]
  (->> ids
    (sort-in-dir sort-dir)
    (paginate offset limit)))

(defn pos-or-zero? [x]
  (or (pos? x) (zero? x)))

(defn parse-float [number]
  (js/parseFloat (string/replace number \, \.)))