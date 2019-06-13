(ns ethlance.ui.page.candidates
  (:require
   [taoensso.timbre :as log]
   [district.ui.component.page :refer [page]]

   [ethlance.shared.enumeration.currency-type :as enum.currency]

   ;; Ethlance Components
   [ethlance.ui.component.main-layout :refer [c-main-layout]]
   [ethlance.ui.component.rating :refer [c-rating]]
   [ethlance.ui.component.tag :refer [c-tag c-tag-label]]
   [ethlance.ui.component.radio-select :refer [c-radio-select c-radio-search-filter-element]]
   [ethlance.ui.component.search-input :refer [c-chip-search-input]]
   [ethlance.ui.component.currency-input :refer [c-currency-input]]
   [ethlance.ui.component.inline-svg :refer [c-inline-svg]]))


(defn c-candidate-search-filter []
  [:div.search-filter "search-filter"])


(defn c-candidate-listing []
  [:div.listing "listing"])


(defmethod page :route.user/candidates []
  (let []
    (fn []
      [c-main-layout {:container-opts {:class :candidates-main-container}}
       [c-candidate-search-filter]
       [:div.listing {:key "listing"}
        [c-chip-search-input {:default-chip-listing #{"C++" "Python"}}]
        [c-candidate-listing]]])))
