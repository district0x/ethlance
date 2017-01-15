(ns ethlance.components.skills-chips
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.misc :refer [row-plain]]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    ))

(defn skills-chips []
  (let [all-skills (subscribe [:app/skills])
        show-all? (r/atom false)]
    (fn [{:keys [:selected-skills :on-touch-tap :always-show-all? :max-count]
          :or {max-count 5}}]
      [row-plain
       {:middle "xs"
        :style styles/chip-list-row}
       (doall
         (for [skill-id (if (and (< max-count (count selected-skills))
                                 (not @show-all?)
                                 (not always-show-all?))
                          (take max-count selected-skills)
                          selected-skills)]
           (let [skill-name (get-in @all-skills [skill-id :skill/name])]
             (when (seq skill-name)
               [ui/chip
                (r/merge-props
                  {:key skill-id
                   :style styles/chip-in-list}
                  (when on-touch-tap
                    {:on-touch-tap #(on-touch-tap skill-id skill-name)}))
                skill-name]))))
       (when (and (< max-count (count selected-skills))
                  (not @show-all?))
         [ui/chip
          {:key :more
           :style styles/chip-in-list
           :on-touch-tap #(reset! show-all? true)}
          [:b (str "+" (- (count selected-skills) max-count))]])])))