(ns ethlance.ui.util.dates
  (:require

    [district.format :as format]
    [cljs-time.core :as t-core]
    [cljs-time.coerce :as t-coerce]))

(defn relative-ago [get-date-field data]
  (format/time-ago (t-core/minus (t-core/now) (t-coerce/from-long (get-date-field data)))))

(defn formatted-date [get-date-field data]
  (format/format-date (t-coerce/from-long (get-date-field data))))
