(ns ethlance.components.misc
  (:require
    [cljs-react-material-ui.icons :as icons]
    [cljs-react-material-ui.reagent :as ui]
    [cljs-web3.core :as web3]
    [ethlance.components.list-pagination :refer [list-pagination]]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [medley.core :as medley]
    [goog.string :as gstring]
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

(defn ether-field [{:keys [:on-change :default-value :form-key :field-key] :as props}]
  (let [validator u/pos-or-zero?]
    [ui/text-field
     (r/merge-props
       (dissoc props :form-key :field-key)
       {:on-change (fn [e val]
                     (let [val (web3/to-wei (u/parse-float val) :ether)]
                       (dispatch [:form/value-changed form-key field-key val validator])))
        :error-text (when-not (validator default-value)
                      "Invalid number")
        :default-value (web3/from-wei default-value :ether)})]))

(defn text-field [{:keys [:max-length-key :min-length-key] :as props}]
  (let [eth-config (subscribe [:eth/config])]
    (fn [{:keys [:default-value :on-change :form-key :field-key]}]
      (let [min-length (get @eth-config min-length-key 0)
            max-length (get @eth-config max-length-key)
            validator #(<= min-length (count %1) max-length)
            valid? (validator default-value)]
        [ui/text-field
         (r/merge-props
           {:on-change #(dispatch [:form/value-changed form-key field-key %2 validator])
            :error-text (when-not valid?
                          (if (pos? min-length)
                            (gstring/format "Write between %s and %s characters" min-length max-length)
                            "Text is too long"))}
           (dissoc props :max-length-key :form-key :field-key :min-length-key))]))))

(defn textarea [props]
  [text-field
   (r/merge-props
     {:rows 4
      :full-width true
      :multi-line true}
     props)])

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

(defn register-required-body [text label href]
  [center-layout
   [paper
    [row
     {:middle "xs" :center "xs"}
     [col {:xs 12}
      text]
     [col {:xs 12}
      [ui/raised-button
       {:primary true
        :href href
        :label label
        :style styles/margin-top-gutter-less}]]]]])

(defn register-freelancer-required []
  )

(defn register-employer-required []
  )

(defmulti register-required identity)

(defmethod register-required :user/employer? []
  [register-required-body
   "Your address must be registered as an employer to see this page "
   "Become Employer"
   (u/path-for :employer/create)])

(defmethod register-required :user/freelancer? []
  [register-required-body
   "Your address must be registered as a freelancer to see this page "
   "Become Freelancer"
   (u/path-for :freelancer/create)])

(defn user-only-page []
  (let [prev-user-id (r/atom nil)
        user (subscribe [:db/active-user])]
    (fn [{:keys [:user-role-pred :on-user-change]} & children]
      (let [{:keys [:user/id]} @user]
        (when-not (= @prev-user-id id)
          (reset! prev-user-id id)
          (when on-user-change
            (on-user-change id)))
        (when id
          (if (get @user user-role-pred)
            (into [:div] children)
            (register-required user-role-pred)))))))

(defn freelancer-only-page [props & children]
  (let [[props children] (u/parse-props-children props children)]
    (into [user-only-page (merge {:user-role-pred :user/freelancer?} props)] children)))

(defn employer-only-page [props & children]
  (let [[props children] (u/parse-props-children props children)]
    (into [user-only-page (merge {:user-role-pred :user/employer?} props)] children)))

(defn subheader [title]
  [ui/subheader {:style styles/subheader} title])