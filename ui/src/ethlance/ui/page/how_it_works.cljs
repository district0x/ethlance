(ns ethlance.ui.page.how-it-works
  (:require [district.ui.component.page :refer [page]]
            [ethlance.ui.component.main-layout :refer [c-main-layout]]))

(defmethod page :route.misc/how-it-works []
  (fn []
    [c-main-layout {:container-opts {:class :how-it-works-main-container}}
     "How It Works"]))
