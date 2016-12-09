(ns ethlance.components.misc
  (:require
    [cljs-react-material-ui.icons :as icons]
    [cljs-react-material-ui.reagent :as ui]
    [cljs-web3.core :as web3]
    [ethlance.components.list-pagination :refer [list-pagination]]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [medley.core :as medley]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    ))

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

(defn ether-field [{:keys [:on-change :default-value :form-key] :as props}]
  (let [valid? u/pos-or-zero?]
    [ui/text-field
     (merge
       (dissoc props :form-key)
       (when on-change
         {:on-change (fn [e val]
                       (let [val (u/parse-float val)]
                         (dispatch [:form/set-invalid form-key (not (valid? val))])
                         (on-change e (web3/to-wei val :ether))))})
       (when default-value
         {:default-value (web3/from-wei default-value :ether)
          :error-text (when-not (valid? default-value)
                        "Invalid number")}))]))

(defn textarea [{:keys [:max-length-key] :as props}]
  (let [eth-config (subscribe [:eth/config])
        valid? #(< (count %1) (get %2 max-length-key))]
    (fn [{:keys [:default-value :on-change :form-key]}]
      [ui/text-field
       (merge
         {:rows 4
          :full-width true
          :multi-line true}
         (dissoc props :max-length-key :form-key)
         (when default-value
           {:error-text (when-not (valid? default-value @eth-config)
                          "Text is too long")})
         (when on-change
           {:on-change (fn [e val]
                         (dispatch [:form/set-invalid form-key (not (valid? default-value @eth-config))])
                         (on-change e val))}))])))

(defn send-button [props]
  [row-plain
   {:end "xs"
    :style styles/form-item}
   [ui/raised-button
    (merge
      {:label "Send"
       :icon (icons/content-send)
       :label-position :before
       :primary true}
      props)]])