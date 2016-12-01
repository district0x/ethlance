(ns ethlance.components.skills-chips
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.layout :refer [col row paper]]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    ))

(def max-count 7)

(defn skills-chips []
  (let [all-skills (subscribe [:app/skills])
        show-all? (r/atom false)]
    (fn [selected-skills]
      [row
       {:middle "xs"
        :style styles/skill-chips-row}
       (for [skill-id (if (and (< max-count (count selected-skills))
                               (not @show-all?))
                        (take max-count selected-skills)
                        selected-skills)]
         [ui/chip
          {:key skill-id
           :style styles/skill-chip}
          (get-in @all-skills [skill-id :skill/name])])
       (when (and (< max-count (count selected-skills))
                  (not @show-all?))
         [:span {:style styles/more-text
                 :on-click #(reset! show-all? true)} "More"])])))