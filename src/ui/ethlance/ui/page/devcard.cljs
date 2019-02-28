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
      [c-ethlance-logo {}]
      [c-button {:on-click #(.alert js/console "Hello!")}
       [:span "test"]]])))
