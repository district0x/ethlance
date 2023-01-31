(ns ethlance.ui.page.about
  (:require [district.ui.component.page :refer [page]]
            [ethlance.ui.component.main-layout :refer [c-main-layout]]))

(defmethod page :route.misc/about []
  (fn []
    [c-main-layout {:container-opts {:class :about-main-container}}
     "About"]))
