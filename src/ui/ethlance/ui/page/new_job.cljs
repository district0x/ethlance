(ns ethlance.ui.page.new-job
  (:require
   [taoensso.timbre :as log]
   [district.ui.component.page :refer [page]]

   ;; Ethlance Components
   [ethlance.ui.component.button :refer [c-button c-button-icon-label c-button-label]]
   [ethlance.ui.component.circle-button :refer [c-circle-icon-button]]
   [ethlance.ui.component.currency-input :refer [c-currency-input]]
   [ethlance.ui.component.inline-svg :refer [c-inline-svg]]
   [ethlance.ui.component.main-layout :refer [c-main-layout]]
   [ethlance.ui.component.radio-select :refer [c-radio-select c-radio-search-filter-element]]
   [ethlance.ui.component.rating :refer [c-rating]]
   [ethlance.ui.component.search-input :refer [c-chip-search-input]]
   [ethlance.ui.component.table :refer [c-table]]
   [ethlance.ui.component.tabular-layout :refer [c-tabular-layout]]
   [ethlance.ui.component.tag :refer [c-tag c-tag-label]]
   [ethlance.ui.component.profile-image :refer [c-profile-image]]
   [ethlance.ui.component.carousel :refer [c-carousel c-feedback-slide]]
   [ethlance.ui.component.text-input :refer [c-text-input]]
   [ethlance.ui.component.select-input :refer [c-select-input]]
   [ethlance.ui.component.textarea-input :refer [c-textarea-input]]))


(defmethod page :route.job/new []
  (let []
    (fn []
      [c-main-layout {:container-opts {:class :new-job-main-container}}
       "New Job Main Container"])))
