(ns ethlance.components.search
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.icons :as icons]
    [ethlance.components.misc :as misc :refer [col row paper-thin row-plain a currency]]
    [ethlance.components.skills-chip-input :refer [skills-chip-input]]
    [ethlance.styles :as styles]
    [re-frame.core :refer [subscribe dispatch]]))

(defn skills-input [{:keys [:selected-skills-subscribe :selected-skills-or-subscribe :open-subscribe]}]
  (let [xs-sm-width? (subscribe [:window/xs-sm-width?])
        xs-width? (subscribe [:window/xs-width?])
        open? (subscribe open-subscribe)
        selected-skills-or (subscribe selected-skills-or-subscribe)
        selected-skills (subscribe selected-skills-subscribe)]
    (fn [{:keys [:skills-hint-text :skills-and-hint-text :skills-or-hint-text :skills-and-floating-label-text
                 :skills-or-floating-label-text :on-toggle-open]}]
      (let [icon (if @open? icons/arrow-up-drop-circle-outline icons/arrow-down-drop-circle-outline)]
        [paper-thin
         {:style styles/position-relative}
         [row-plain
          {:middle "xs"}
          [skills-chip-input
           {:value @selected-skills
            :hint-text (if @open? skills-and-hint-text skills-hint-text)
            :floating-label-text (if @open? skills-and-floating-label-text "Skills")
            :on-change #(dispatch [:form.search/set-value :search/skills %1])
            :style (if @xs-sm-width? {:width "calc(100% - 48px)"} {})
            :full-width (not @xs-sm-width?)
            :hint-style (if @xs-width? styles/skills-input-xs-hint-size {})}]
          [ui/icon-button
           {:tooltip "Advanced Search"
            :style (if @xs-sm-width?
                     {:margin-top 10}
                     {:position :absolute
                      :right 5
                      :top 5})
            :on-touch-tap #(on-toggle-open (not @open?))}
           (icon {:color styles/fade-color})]]
         (when @open?
           [skills-chip-input
            {:value @selected-skills-or
             :hint-text skills-or-hint-text
             :floating-label-text skills-or-floating-label-text
             :hint-style (if @xs-width? styles/skills-input-xs-hint-size {})
             :on-change #(dispatch [:form.search/set-value :search/skills-or %1])}])]))))
