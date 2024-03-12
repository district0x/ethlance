(ns ethlance.ui.util.dates
  (:require
    [cljs-time.coerce :as t-coerce]
    [cljs-time.core :as t-core]
    [district.format :as format]))


(defn relative-ago
  [get-date-field data]
  (format/time-ago (t-core/minus (t-core/now) (t-coerce/from-long (get-date-field data)))))


(defn formatted-date
  ([data] (formatted-date identity data))
  ([get-date-field data]
   (when (not (nil? (get-date-field data)))
     (format/format-date (t-coerce/from-long (get-date-field data))))))
