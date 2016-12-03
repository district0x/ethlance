(ns ethlance.components.skills-chip-input
  (:require [ethlance.utils :as u]
            [ethlance.components.misc :as layout :refer [col row paper-thin row-plain]]
            [ethlance.components.chip-input :refer [chip-input]]
            [re-frame.core :refer [subscribe dispatch]]))

(defn skills-chip-input []
  (let [skills (subscribe [:app/skills])]
    (fn [{:keys [value on-change] :as props}]
      (let [skills-data-source (u/create-data-source @skills :skill/name)
            selected-skills-ds (u/create-data-source (select-keys @skills value) :skill/name)]
        [chip-input
         (merge
           {:default-value selected-skills-ds
            :dataSource skills-data-source
            :dataSourceConfig u/data-source-config
            :new-chip-key-codes []
            :full-width true
            :floating-label-text "Skills"
            :max-search-results 10}
           (dissoc props :value)
           {:on-change (fn [result]
                         (when (fn? on-change)
                           (on-change (u/data-source-values result))))})]))))
