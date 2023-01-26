(ns ethlance.ui.page.home
  "Main landing page for ethlance website"
  (:require [district.ui.component.page :refer [page]]
            [ethlance.ui.component.splash-layout :refer [c-splash-layout]]))

(defmethod page :route/home []
  (fn []
    [c-splash-layout]))
