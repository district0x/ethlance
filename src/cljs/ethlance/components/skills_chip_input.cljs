(ns ethlance.components.skills-chip-input
  (:require
    [ethlance.components.chip-input :refer [chip-input]]
    [ethlance.components.misc :as layout :refer [col row paper-thin row-plain]]
    [ethlance.components.validated-chip-input :refer [validated-chip-input]]
    [ethlance.utils :as u]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn skills-chip-input []
  (let [skills (subscribe [:app/skills])]
    (fn [{:keys [:validated?] :as props}]
      [(if validated? validated-chip-input chip-input)
       (r/merge-props
         {:all-items @skills
          :all-items-val-key :skill/name
          :floating-label-text "Skills"}
         (dissoc props :validated?))])))
