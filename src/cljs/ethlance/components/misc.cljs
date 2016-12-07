(ns ethlance.components.misc
  (:require
    [cljs-react-material-ui.icons :as icons]
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.styles :as styles]
    [ethlance.components.list-pagination :refer [list-pagination]]
    [ethlance.utils :as u]
    [reagent.core :as r]
    [medley.core :as medley]))

(def col (r/adapt-react-class js/ReactFlexboxGrid.Col))
(def row (r/adapt-react-class js/ReactFlexboxGrid.Row))
(def grid (r/adapt-react-class js/ReactFlexboxGrid.Grid))

(u/set-default-props! (aget js/MaterialUI "TableHeader") {:adjust-for-checkbox false
                                                          :display-select-all false})
(u/set-default-props! (aget js/MaterialUI "TableBody") {:display-row-checkbox false})
(u/set-default-props! (aget js/MaterialUI "TableRow") {:selectable false})
(u/set-default-props! (aget js/MaterialUI "Table") {:selectable false})
(u/set-default-props! (aget js/MaterialUI "TableFooter") {:adjust-for-checkbox false})

(def row-plain (u/create-with-default-props row {:style styles/row-no-margin}))

(defn paper [props & children]
  (let [[props children] (u/parse-props-children props children)]
    [ui/paper
     (dissoc props :loading? :inner-style)
     [ui/linear-progress {:mode :indeterminate
                          :style {:visibility (if (:loading? props) :visible :hidden)}}]
     (into [] (concat [:div {:style (merge styles/paper-secton (:inner-style props))}]
                      children))]))

(def paper-thin (u/create-with-default-props paper {:inner-style styles/paper-secton-thin}))

(defn search-layout [filter-sidebar skills-input search-results]
  [row
   [col {:xs 4}
    filter-sidebar]
   [col {:xs 8}
    [row
     [col {:xs 12}
      skills-input]
     [col {:xs 12}
      search-results]]]])

(defn country-marker [{:keys [row-props country]}]
  [row-plain
   (r/merge-props
     {:middle "xs"}
     row-props)
   (icons/maps-place {:color styles/fade-color :style styles/location-icon-small})
   (u/country-name country)])

(def status-chip (u/create-with-default-props ui/chip {:label-style {:color "#FFF" :font-weight :bold}
                                                       :style {:margin-right 5 :margin-bottom 5}}))


(def hr-small (u/create-with-default-props :div {:style styles/hr-small}))
(def hr (u/create-with-default-props :div {:style styles/hr}))

(defn line
  ([body]
   [:div {:style styles/line} body])
  ([label body]
   [:div {:style styles/line} [:span label ": "] [:b body]]))

(defn a
  ([{:keys [route-params route underline-hover?] :as props} body]
   [:a
    (r/merge-props
      {:style {:color (:primary1-color styles/palette)}
       :class (when underline-hover? "hoverable")
       :href (when-not (some nil? (vals route-params))
               (medley/mapply u/path-for route route-params))
       :on-click #(.stopPropagation %)}
      (dissoc props :route-params :route)) body]))

(defn create-table-pagination [list-pagination-props]
  [ui/table-footer
   [ui/table-row
    [ui/table-row-column
     {:col-span 99
      :style {:text-align :right :padding-right 0}}
     [list-pagination list-pagination-props]]]])

(defn create-no-items-row [text & [loading?]]
  [ui/table-row
   [ui/table-row-column
    {:col-span 99 :style styles/text-center}
    (if-not loading? text "Loading...")]])

(defn center-layout [& children]
  [row {:center "xs"}
   (into [] (concat [col {:lg 8 :style styles/text-left}]
                    children))])