(ns ethlance.components.layout
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.styles :as styles]
    [reagent.core :as r]
    [ethlance.utils :as u]))

(def col (r/adapt-react-class js/ReactFlexboxGrid.Col))
(def row (r/adapt-react-class js/ReactFlexboxGrid.Row))
(def grid (r/adapt-react-class js/ReactFlexboxGrid.Grid))

(defn paper [props & children]
  (let [[props children] (u/parse-props-children props children)]
    [ui/paper
     (dissoc props :loading?)
     [ui/linear-progress {:mode :indeterminate
                          :style {:visibility (if (:loading? props) :visible :hidden)}}]
     (into [] (concat [:div {:style styles/paper-secton}]
                      children))]))