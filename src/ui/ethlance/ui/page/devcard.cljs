(ns ethlance.ui.page.devcard
  "Development Page for showing off different reagent components"
  (:require
   [district.ui.component.page :refer [page]]

   ;; Ethlance Components
   [ethlance.ui.component.ethlance-logo :refer [c-ethlance-logo]]
   [ethlance.ui.component.button :refer [c-button]]))


(defmethod page :route.devcard/index []
  (let []
    (fn []
     [:div.page-devcard
      [:div.devcard-header
       [:b "Ethlance Components"]]
      [c-ethlance-logo {:color :secondary}]
      [c-button {:on-click (fn [e] (println "Test"))}
       [:span "test"]]])))
