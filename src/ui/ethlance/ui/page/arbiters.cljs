(ns ethlance.ui.page.arbiters
  "General Arbiter Listings on ethlance"
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


(defn c-arbiter-search-filter []
  [:div.search-filter "search-filter"])


(defn c-arbiter-listing []
  [:div.listing "listing"])


(defmethod page :route.user/arbiters []
  (let []
    (fn []
      [c-main-layout {:container-opts {:class :arbiters-main-container}}
       [c-arbiter-search-filter]
       [:div.arbiter-listing {:key "listing"}
        [c-chip-search-input {:default-chip-listing #{"C++" "Python"}}]
        [c-arbiter-listing]]])))
