(ns ethlance.components.category-select-field
  (:require [cljs-react-material-ui.reagent :as ui]))

(def categories
  {0 "All Categories"
   1 "Web, Mobile & Software Dev"
   2 "IT & Networking"
   3 "Data Science & Analytics"
   4 "Design & Creative"
   5 "Writing"
   6 "Translation"
   7 "Legal"
   8 "Admin Support"
   9 "Customer Service"
   10 "Sales & Marketing"
   11 "Accounting & Consulting"
   12 "Other"})

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
