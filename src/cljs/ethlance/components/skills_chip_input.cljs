(ns ethlance.components.skills-chip-input
  (:require [ethlance.utils :as u]
            [ethlance.components.misc :as layout :refer [col row paper-thin row-plain]]
            [ethlance.components.chip-input :refer [chip-input]]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]))

(defn skills-chip-input []
  (let [skills (subscribe [:app/skills])]
    (fn [props]
      [chip-input
       (r/merge-props
         {:all-items @skills
          :all-items-val-key :skill/name
          :floating-label-text "Skills"}
         props)])))
