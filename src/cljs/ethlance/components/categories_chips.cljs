(ns ethlance.components.categories-chips
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.misc :refer [col row]]
    [ethlance.constants :as constants]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [reagent.core :as r]
    ))

(defn categories-chips [{:keys [value]}]
  [row
   {:middle "xs"
    :style styles/chip-list-row}
   (for [category-id value]
     [ui/chip
      {:key category-id
       :style styles/chip-in-list}
      (when (< category-id (count constants/languages))
        (constants/categories category-id))])])


