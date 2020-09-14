(ns district.ui.router.utils
  (:require
    [district.ui.router.subs :as subs]
    [re-frame.core :refer [subscribe]])
  (:refer-clojure :exclude [resolve]))

(defn resolve [name & [params query]]
  @(subscribe [::subs/resolve name params query]))

(defn match [path]
  @(subscribe [::subs/match path]))
