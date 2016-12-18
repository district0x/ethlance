(ns ethlance.components.country-select-field
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [medley.core :as medley]
    [ethlance.constants :refer [countries]]))

(defn country-select-field [{:keys [:value] :as props}]
  [ui/select-field
   (merge
     {:floating-label-text "Country"
      :hint-text "Choose Country"}
     (dissoc props :no-all-categories?)
     {:value (when (pos? value)
               value)})
   (for [[id name] (medley/indexed countries)]
     [ui/menu-item
      {:value (inc id)
       :primary-text name
       :key (inc id)}])])
