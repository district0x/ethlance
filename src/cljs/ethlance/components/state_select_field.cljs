(ns ethlance.components.state-select-field
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [medley.core :as medley]
    [ethlance.constants :refer [united-states]]))

(defn state-select-field [{:keys [:value] :as props}]
  [ui/select-field
   (merge
     {:floating-label-text "State"
      :hint-text "Choose State"}
     props
     {:value (when (pos? value)
               value)})
   (for [[id name] (medley/indexed united-states)]
     [ui/menu-item
      {:value (inc id)
       :primary-text name
       :key (inc id)}])])
