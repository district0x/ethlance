(ns ethlance.ui.page.home
  "Main landing page for ethlance website"
  (:require
   [district.ui.component.page :refer [page]]
   [ethlance.ui.component.splash-layout :refer [c-splash-layout]]
   [ethlance.ui.component.main-layout :refer [c-main-layout]]))


(defmethod page :route/home []
  (let []
    (fn []
      [c-splash-layout])))
