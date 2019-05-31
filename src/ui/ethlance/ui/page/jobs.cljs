(ns ethlance.ui.page.jobs
  "General Job Listings on ethlance"
  (:require
   [district.ui.component.page :refer [page]]
   [ethlance.ui.component.main-layout :refer [c-main-layout]]))


(defmethod page :route.job/jobs []
  (let []
    (fn []
      [c-main-layout])))
