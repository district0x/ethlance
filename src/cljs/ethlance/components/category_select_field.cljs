(ns ethlance.components.category-select-field
  (:require [cljs-react-material-ui.reagent :as ui]
            [ethlance.constants :refer [categories]]))

(defn category-select-field [{:keys [:no-all-categories?] :as props}]
  [ui/select-field
   (merge
     {:floating-label-text "Category"
      :hint-text "Choose Category"}
     (dissoc props :no-all-categories?))
   (for [[id name] (if no-all-categories? (dissoc categories 0) categories)]
     [ui/menu-item
      {:value id
       :primary-text name
       :key id}])])
