(ns ethlance.components.currency-select-field
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.constants :as constants]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn currency-select-field [props]
  [ui/select-field
   (r/merge-props
     {:auto-width true
      :style {:width 45}}
     props)
   (for [[key text] constants/currencies]
     [ui/menu-item
      {:value key
       :primary-text (str text " " (name (constants/currency-id->code key)))
       :label text
       :key key}])])
