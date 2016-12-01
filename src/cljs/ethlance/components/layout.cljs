(ns ethlance.components.layout
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.styles :as styles]
    [reagent.core :as r]))

(def col (r/adapt-react-class js/ReactFlexboxGrid.Col))
(def row (r/adapt-react-class js/ReactFlexboxGrid.Row))
(def grid (r/adapt-react-class js/ReactFlexboxGrid.Grid))

(defn paper [& children]
  (into [] (concat [ui/paper {:style styles/paper-secton}]
                   children)))