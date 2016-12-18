(ns ethlance.components.languages-chips
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.misc :refer [col row]]
    [ethlance.constants :as constants]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [reagent.core :as r]
    ))

(defn languages-chips [{:keys [value]}]
  [row
   {:middle "xs"
    :style styles/chip-list-row}
   (for [language-id value]
     [ui/chip
      {:key language-id
       :style styles/chip-in-list}
      (when (< language-id (count constants/languages))
        (constants/languages (dec language-id)))])])


