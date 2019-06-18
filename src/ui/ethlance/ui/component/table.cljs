(ns ethlance.ui.component.table
  (:require
   [reagent.core :as r]))


(defn c-table [{:keys [headers]} & rows]
  (let []
    [:div.ethlance-table
     [:table
      [:tr
       (doall
        (for [header headers]
         [:th header]))]
      (doall
       (for [row rows]
        [:tr
         (for [elem row]
          [:td elem])]))]]))
