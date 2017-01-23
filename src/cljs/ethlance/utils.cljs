(ns ethlance.utils
  (:require
    [bidi.bidi :as bidi]
    [camel-snake-kebab.core :as cs :include-macros true]
    [camel-snake-kebab.extras :refer [transform-keys]]
    [cemerick.url :as url]
    [cljs-react-material-ui.reagent :as ui]
    [cljs-time.coerce :refer [to-date-time to-long to-local-date-time]]
    [cljs-time.core :as t :refer [date-time to-default-time-zone]]
    [cljs-time.format :as time-format]
    [cljs-web3.core :as web3]
    [cljs.reader :as reader]
    [clojure.core.async :refer [chan <! >!]]
    [clojure.data :as data]
    [clojure.string :as string]
    [ethlance.constants :as constants]
    [ethlance.routes :refer [routes]]
    [ethlance.styles :as styles]
    [goog.crypt :as crypt]
    [goog.crypt.Md5 :as Md5]
    [goog.format.EmailAddress :as email-address]
    [goog.string :as gstring]
    [goog.string.format]
    [medley.core :as medley]
    [reagent.core :as r]
    [clojure.set :as set])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn path-for [& args]
  (str "#" (apply bidi/path-for routes args)))

(defn current-location-hash []
  (let [hash (-> js/document
               .-location
               .-hash
               (string/split #"\?")
               first
               (string/replace "#" ""))]
    (if (empty? hash) "/" hash)))

(defn set-location-hash! [s]
  (set! (.-hash js/location) s))

(defn current-url []
  (url/url (string/replace (.-href js/location) "#" "")))

(defn set-location-query! [query-params]
  (set-location-hash!
    (str "#" (current-location-hash)
         (when-let [query (url/map->query query-params)]
           (str "?" query)))))

(defn add-to-location-query! [query-params]
  (let [current-query (:query (current-url))
        new-query (merge current-query (->> query-params
                                         (medley/map-keys constants/keyword->query)
                                         (medley/remove-keys nil?)))]
    (set-location-query! new-query)))

(defn current-url-query []
  (->> (:query (current-url))
    (medley/map-keys (set/map-invert ethlance.constants/keyword->query))
    (medley/remove-keys nil?)
    (map (fn [[k v]]
           (if-let [f (constants/query-parsers k)]
             {k (f v)}
             {k v})))
    (into {})))

(defn nav-to! [route route-params]
  (set-location-hash! (medley/mapply path-for route route-params)))

(defn ns+name [x]
  (when x
    (str (when-let [n (namespace x)] (str n "/")) (name x))))

(defn match-current-location []
  (bidi/match-route routes (current-location-hash)))

(defn target-value [e]
  (aget e "target" "value"))

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

(defn big-num? [x]
  (and x (aget x "toNumber")))

(defn big-num-pos? [x]
  (when x
    (.greaterThan x 0)))

(defn big-num-neg? [x]
  (when x
    (.isNegative x)))

(defn eth->to-wei [x]
  (if (and x (aget x "toNumber"))
    (web3/to-wei x :ether)
    x))

(defn replace-comma [x]
  (string/replace x \, \.))

(defn empty-string? [x]
  (and (string? x) (empty? x)))

(defn non-neg-ether-value? [x & [{:keys [:allow-empty?]}]]
  (try
    (when (and (not allow-empty?) (empty-string? x))
      (throw (js/Error.)))
    (let [value (web3/to-wei (if (string? x) (replace-comma x) x) :ether)]
      (and
        (or (and (string? value)
                 (not (= "-" (first value))))
            (and (big-num? value)
                 (not (big-num-neg? value))))))
    (catch :default e
      false)))

(def non-neg-or-empty-ether-value? #(non-neg-ether-value? % {:allow-empty? true}))

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

(defn md5-bytes [s]
  (let [container (doto (goog.crypt.Md5.)
                    (.update s))]
    (.digest container)))

(defn md5 [s]
  (crypt/byteArrayToHex (md5-bytes s)))

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

(defn map->data-source [m val-key]
  (map (fn [[k v]] {"text" (get v val-key) "value" k}) (into [] m)))

(defn coll->data-source [coll]
  (mapv (fn [[k v]] {"text" v "value" (inc k)}) coll))

(defn results-coll->data-source [results all-items]
  (mapv (fn [k] {"text" (nth all-items (dec k)) "value" k}) results))

(def data-source-config {"text" "text" "value" "value"})

(defn data-source-values [values]
  (set (map :value (js->clj values :keywordize-keys true))))

(defn assoc-key-as-value [key-name m]
  (into {} (map (fn [[k v]]
                  {k (assoc v key-name k)}) m)))

(defn timestamp-js->sol [x]
  (/ x 1000))

(defn timestamp-sol->js [x]
  (* x 1000))

(defn big-num->date-time [big-num]
  (when (big-num-pos? big-num)
    (to-default-time-zone (to-date-time (timestamp-sol->js (.toNumber big-num))))))

(defn format-datetime [date]
  (when date
    (time-format/unparse-local (time-format/formatters :rfc822) date)))

(defn format-date [date]
  (when date
    (time-format/unparse-local (time-format/formatter "EEE, dd MMM yyyy Z") date)))

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

(defn pluralize [text count]
  (str text (when (not= count 1) "s")))

(defn country-name [country-id]
  (when (and (pos? country-id)
             (<= country-id (count constants/countries)))
    (nth constants/countries (dec country-id))))

(defn state-name [state-id]
  (when (and (pos? state-id)
             (<= state-id (count constants/united-states)))
    (nth constants/united-states (dec state-id))))

(defn rating->star [rating]
  (/ (or rating 0) 20))

(defn star->rating [star]
  (* (or star 0) 20))

(defn rand-str [n & [{:keys [:lowercase-only?]}]]
  (let [chars-between #(map char (range (.charCodeAt %1) (inc (.charCodeAt %2))))
        chars (concat (when-not lowercase-only? (chars-between \0 \9))
                      (chars-between \a \z)
                      (when-not lowercase-only? (chars-between \A \Z))
                      (when-not lowercase-only? [\_]))
        password (take n (repeatedly #(rand-nth chars)))]
    (reduce str password)))

(defn create-with-default-props [component default-props]
  (fn [props & children]
    (let [[props children] (parse-props-children props children)]
      (into [] (concat
                 [component (r/merge-props default-props props)]
                 children)))))

(defn gravatar-url [hash & [user-id]]
  (let [valid? (= (count hash) 32)]
    (gstring/format "http://s.gravatar.com/avatar/%s?s=300&d=retro%s"
                    (if valid? hash user-id)
                    (if valid? "" "&f=y"))))

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
  (case dir
    :desc (sort-desc coll)
    :asc (sort coll)
    coll))

(defn sort-paginate-ids [{:keys [offset limit sort-dir]} ids]
  (if (and offset limit)
    (->> ids
      (sort-in-dir sort-dir)
      (paginate offset limit))
    ids))

(defn parse-float [number]
  (if (string? number)
    (js/parseFloat (replace-comma number))
    number))

(defn pos-or-zero? [x]
  (let [x (parse-float x)]
    (or (pos? x) (zero? x))))

(defn get-time [x]
  (.getTime x))

(defn week-ago []
  (t/minus (t/today-at-midnight) (t/weeks 1)))

(defn remove-zero-chars [s]
  (string/join (take-while #(< 0 (.charCodeAt % 0)) s)))

(defn alphanumeric? [x]
  (re-matches #"[a-zA-Z0-9 ]*" x))

(defn etherscan-url [address]
  (gstring/format "https://etherscan.io/address/%s" address))

(defn prepend-address-zeros [address]
  (let [n (- 42 (count address))]
    (if (pos? n)
      (->> (subs address 2)
        (str (string/join (take n (repeat "0"))))
        (str "0x"))
      address)))

(defn unzip-map [m]
  [(keys m) (vals m)])

(defn conj-colls [colls coll]
  (map (fn [[i c]]
         (conj c (nth coll i))) (medley/indexed colls)))

(defn get-window-width-size [width]
  (cond
    (>= width 1200) 3
    (>= width 1024) 2
    (>= width 768) 1
    :else 0))

(defn format-currency [value currency & [{:keys [:full-length?]}]]
  (let [currency (keyword currency)
        value (or value 0)
        value (if (and full-length? (= currency :eth)) value (gstring/format "%.3f" value))]
    (case currency
      :usd (str (constants/currencies :usd) value)
      (str value (constants/currencies currency)))))

(defn united-states? [country-id]
  (= 232 country-id))

(defn empty-or-valid-email? [s]
  (or (empty? s)
      (email-address/isValidAddress s)))

(defn split-include-empty [s re]
  (butlast (string/split (str s " ") re)))