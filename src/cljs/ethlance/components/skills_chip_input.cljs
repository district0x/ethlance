(ns ethlance.components.skills-chip-input
  (:require
    [ethlance.components.chip-input :refer [chip-input]]
    [ethlance.components.validated-chip-input :refer [validated-chip-input]]
    [ethlance.utils :as u]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [ethlance.styles :as styles]
    [ethlance.constants :as constants]))

(defn skills-chip-input []
  (let [skills-loaded? (subscribe [:db/skills-loaded?])]
    (fn [{:keys [:validated?] :as props}]
      @skills-loaded?
      [(if validated? validated-chip-input chip-input)
       (r/merge-props
         {:all-items constants/skills
          :floating-label-text "Skills"
          :chip-backgroud-color styles/skills-chip-color
          :max-search-results 50
          :menu-props styles/chip-input-menu-props}
         (dissoc props :validated?))])))
