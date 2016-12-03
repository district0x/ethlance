(ns ethlance.components.category-select-field
  (:require [cljs-react-material-ui.reagent :as ui]
            [ethlance.constants :refer [categories]]))

(defn category-select-field []
  (fn [props]
    [ui/select-field
     (merge
       {:floating-label-text "Category"}
       props)
     (for [[id name] categories]
       [ui/menu-item
        {:value id
         :primary-text name
         :key id}])]))
