(ns ethlance.ui.component.table
  (:require
   [reagent.core :as r]))


(defn c-table
  ""
  [{:keys [headers]} & rows]
  (let []
    [:div.ethlance-table
     [:table
      [:tr
       (doall
        (for [[i header] (map-indexed vector headers)]
          ^{:key (str "header-" i)}
          [:th header]))]
      (doall
       (for [[i row] (map-indexed vector rows)]
         ^{:key (str "row-" i)}
         [:tr
          (for [[i elem] (map-indexed vector row)]
            ^{:key (str "elem-" i)}
            [:td elem])]))]]))
