(ns ethlance.components.misc
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [cljs-web3.core :as web3]
    [clojure.string :as string]
    [district.ui.mobile.subs :as mobile-subs]
    [ethlance.components.currency-select-field :refer [currency-select-field]]
    [ethlance.components.icons :as icons]
    [ethlance.components.linkify :refer [linkify]]
    [ethlance.components.list-pagination :refer [list-pagination]]
    [ethlance.constants :as constants]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [goog.string :as gstring]
    [medley.core :as medley]
    [re-frame.core :refer [subscribe dispatch dispatch-sync]]
    [reagent.core :as r]
    [reagent.impl.template :as tmpl]))

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

(defn paper []
  (let [xs-width (subscribe [:window/xs-width?])
        connection-error? (subscribe [:blockchain/connection-error?])]
    (fn [props & children]
      (let [[props children] (u/parse-props-children props children)]
        [ui/paper
         (dissoc props :loading? :inner-style)
         [ui/linear-progress {:mode :indeterminate
                              :style {:visibility (if (and (:loading? props)
                                                           (not @connection-error?))
                                                    :visible
                                                    :hidden)}
                              :color styles/accent1-color}]
         (into [] (concat [:div {:style (merge (if @xs-width styles/paper-secton-thin
                                                             styles/paper-secton)
                                               styles/word-wrap-break
                                               (:inner-style props))}]
                          children))]))))

(def paper-thin (u/create-with-default-props paper {:inner-style styles/paper-secton-thin}))

(defn search-filter-open-button []
  (let [xs-sm-width? (subscribe [:window/xs-sm-width?])
        xs-width? (subscribe [:window/xs-width?])]
    (fn [props]
      (when @xs-sm-width?
        [row-plain
         {:end "xs"
          :style (if @xs-width? {:margin-top styles/desktop-gutter-less} {})}
         [ui/floating-action-button
          (r/merge-props
            {:style {:margin-bottom -26
                     :margin-right styles/desktop-gutter
                     :z-index 99}}
            props)
          (icons/filter-variant)]]))))

(defn search-layout []
  (let [xs-sm-width? (subscribe [:window/xs-sm-width?])]
    (fn [{:keys [:filter-drawer-props :filter-open-button-props]} filter-sidebar skills-input search-results]
      [row
       (when @xs-sm-width?
         [col {:xs 12}
          [search-filter-open-button filter-open-button-props]])
       (if @xs-sm-width?
         [ui/drawer
          (r/merge-props
            {:width 300
             :docked false
             :open-secondary true}
            filter-drawer-props)
          filter-sidebar]
         [col {:xs 12 :md 4
               :style {:min-height 1250}}
          filter-sidebar])
       [col {:xs 12 :md 8}
        [row
         [col {:xs 12}
          skills-input]
         [col {:xs 12}
          search-results]]]])))

(defn country-marker [{:keys [:row-props :country :state]}]
  [row-plain
   (r/merge-props
     {:middle "xs"}
     row-props)
   (icons/map-marker {:color styles/fade-color :style styles/location-icon-small})
   (str (u/country-name country)
        (when (and (u/united-states? country)
                   (pos? state))
          (str ", " (u/state-name state))))])

(def status-chip (u/create-with-default-props ui/chip {:label-style {:color "#FFF" :font-weight :bold}}))

(def hr-small (u/create-with-default-props :div {:style styles/hr-small}))
(def hr (u/create-with-default-props :div {:style styles/hr}))

(defn line
  ([body]
   [:div {:style styles/line} body])
  ([label body]
   [:div {:style styles/line} [:span label ": "] [:b body]]))

(defn a [{:keys [route-params route underline-hover?] :as props} body]
  [:a
   (r/merge-props
     {:style {:color (:primary1-color styles/palette)}
      :class (when underline-hover? "hoverable")
      :href (when-not (some nil? (vals route-params))
              (medley/mapply u/path-for route route-params))
      :on-click #(.stopPropagation %)}
     (dissoc props :route-params :route)) body])

(defn create-table-pagination [list-pagination-props]
  [ui/table-footer
   [ui/table-row
    [ui/table-row-column
     {:col-span 99
      :style styles/pagination-row-column}
     [row-plain
      {:start "xs"
       :end "sm"}
      [list-pagination list-pagination-props]]]]])

(defn create-no-items-row [text & [loading?]]
  [ui/table-row
   [ui/table-row-column
    {:col-span 99 :style styles/text-center}
    (if-not loading? text "Loading...")]])

(defn center-layout [& children]
  [row {:center "xs"}
   (into [col {:xs 12 :md 10 :lg 9 :style styles/text-left}]
         children)])

(def text-field-base*
  (tmpl/adapt-react-class (aget js/MaterialUI "TextField")
                          ;; Optional...
                          {:synthetic-input
                           ;; A valid map value for `synthetic-input` does two things:
                           ;; 1) It implicitly marks this component class as an input type so that interactive
                           ;;    updates will work without cursor jumping.
                           ;; 2) Reagent defers to its functions when it goes to set a value for the input component,
                           ;;    or signal a change, providing enough data for us to decide which DOM node is our input
                           ;;    node to target and continue processing with that (or any arbitrary behaviour...); and
                           ;;    to handle onChange events arbitrarily.
                           ;;
                           ;;    Note: We can also use an extra hook `on-write` to execute more custom behaviour
                           ;;    when Reagent actually writes a new value to the input node, from within `on-update`.
                           ;;
                           ;;    Note: Both functions receive a `next` argument which represents the next fn to
                           ;;    execute in Reagent's processing chain.
                           {:on-update (fn [next root-node rendered-value dom-value component]
                                         (let [input-node (.querySelector root-node "input")
                                               textarea-nodes (array-seq (.querySelectorAll root-node "textarea"))
                                               textarea-node (when (= 2 (count textarea-nodes))
                                                               ;; We are dealing with EnhancedTextarea (i.e.
                                                               ;; multi-line TextField)
                                                               ;; so our target node is the second <textarea>...
                                                               (second textarea-nodes))
                                               target-node (or input-node textarea-node)]
                                           (when target-node
                                             ;; Call Reagent's input node value setter fn (extracted from input-set-value)
                                             ;; which handles updating of a given <input> element,
                                             ;; now that we have targeted the correct <input> within our component...
                                             (next target-node rendered-value dom-value component
                                                   ;; Also hook into the actual value-writing step,
                                                   ;; since `input-node-set-value doesn't necessarily update values
                                                   ;; (i.e. not dirty).
                                                   {:on-write
                                                    (fn [new-value]
                                                      ;; `blank?` is effectively the same conditional as Material-UI uses
                                                      ;; to update its `hasValue` and `isClean` properties, which are
                                                      ;; required for correct rendering of hint text etc.
                                                      (if (clojure.string/blank? new-value)
                                                        (.setState component #js {:hasValue false :isClean false})
                                                        (.setState component #js {:hasValue true :isClean false})))}))))
                            :on-change (fn [next event]
                                         ;; All we do here is continue processing but with the event target value
                                         ;; extracted into a second argument, to match Material-UI's existing API.
                                         (next event (-> event .-target .-value)))}}))

(defn text-field-base [{:keys [:value] :as props}]
  [text-field-base*
   (r/merge-props
     {:style styles/display-block
      :floating-label-fixed (boolean (seq (str value)))}
     props)])

(defn form-text-field [{:keys [:validator :form-key :field-key :value] :as props}]
  [text-field-base
   (r/merge-props
     (r/merge-props
       {:style styles/display-block
        :floating-label-fixed (boolean (seq (str value)))}
       (dissoc props :validator :form-key :field-key))
     {:on-change #(dispatch [:form/set-value form-key field-key %2 validator])})])

(defn text-field []
  (let [eth-config (subscribe [:eth/config])]
    (fn [{:keys [:value :on-change :max-length-key :min-length-key :max-length :min-length] :as props}]
      (let [min-length (or min-length (get @eth-config min-length-key 0))
            max-length (or max-length (get @eth-config max-length-key))
            validator (u/create-length-validator min-length max-length)
            valid? (validator value)]
        [form-text-field
         (r/merge-props
           {:validator validator
            :error-text (when-not valid?
                          (if (pos? min-length)
                            (gstring/format "Write between %s and %s characters" min-length max-length)
                            "Text is too long"))}
           (dissoc props :max-length-key :min-length-key :min-length :max-length))]))))

(defn url-field []
  (let [eth-config (subscribe [:eth/config])]
    (fn [{:keys [:value :on-change :max-length-key :max-length :allow-empty?] :as props}]
      (let [max-length (or max-length (get @eth-config max-length-key))
            max-length-validator (u/create-length-validator max-length)]
        [form-text-field
         (r/merge-props
           {:validator (every-pred #(u/http-url? % {:allow-empty? true}) max-length-validator)
            :error-text (if-not (max-length-validator value)
                          (gstring/format "URL must be shorter than %s characters" max-length)
                          (when-not (u/http-url? value {:allow-empty? allow-empty?})
                            "Invalid URL"))}
           (dissoc props :allow-empty? :max-length-key :max-length))]))))

(defn ether-field [{:keys [:value :on-change :form-key :field-key :on-change :allow-empty?
                           :only-positive?] :as props}]
  (let [validator (if only-positive? u/pos-ether-value? u/non-neg-ether-value?)]
    [text-field-base
     (r/merge-props
       {:style styles/display-block
        :on-change (fn [e value]
                     (if on-change
                       (on-change e value)
                       (dispatch [:form/set-value
                                  form-key
                                  field-key
                                  value
                                  #(validator % (select-keys props [:allow-empty?]))])))
        :error-text (when-not (validator value (select-keys props [:allow-empty?]))
                      "Invalid value")}
       (dissoc props :form-key :field-key :on-change :allow-empty? :only-positive?))]))

(defn ether-field-with-currency [{:keys [:currency :disabled :currency-style] :as props}]
  [row-plain
   {:bottom "xs"}
   [ether-field (-> props
                  (dissoc :currency :currency-style)
                  (->> (r/merge-props {:style {:display :inline-block
                                               :width 205}})))]
   [:span
    {:style (merge styles/ether-field-currency
                   (when disabled styles/fade-text)
                   currency-style)}
    (u/currency-full-name currency)]])

(defn ether-field-with-currency-select-field [{:keys [:ether-field-props :currency-select-field-props]}]
  [row-plain
   {:bottom "xs"}
   [ether-field
    (r/merge-props
      {:style (if (:full-width ether-field-props)
                {:width "calc(100% - 50px)"}
                {:width 209})}
      ether-field-props)]
   [currency-select-field
    (r/merge-props
      {:style {:margin-left 5}}
      currency-select-field-props)]])

(def textarea (u/create-with-default-props text-field {:rows 4
                                                       :full-width true
                                                       :multi-line true}))

(defn send-button [props]
  [row-plain
   {:end "xs"
    :style styles/form-item}
   [ui/raised-button
    (merge
      {:label "Send"
       :icon (icons/send)
       :label-position :before
       :primary true}
      props)]])

(defn centered-rows [& children]
  [center-layout
   [paper
    [row
     {:middle "xs" :center "xs"}
     (for [[i child] (medley/indexed children)]
       [col {:xs 12 :key i}
        child])]]])

(defn register-required-body [text label href]
  [centered-rows
   text
   [ui/raised-button
    {:primary true
     :href href
     :label label
     :style styles/margin-top-gutter-less}]])

(defmulti register-required identity)

(defmethod register-required :user/employer? []
  [register-required-body
   "Your address must be registered as an employer to see this page "
   "Become Employer"
   (u/path-for :user/edit)])

(defmethod register-required :user/freelancer? []
  [register-required-body
   "Your address must be registered as a freelancer to see this page "
   "Become Freelancer"
   (u/path-for :user/edit)])

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

(defn only-freelancer [props & children]
  (let [[props children] (u/parse-props-children props children)]
    (into [user-only-page (merge {:user-role-pred :user/freelancer?} props)] children)))

(defn only-employer [props & children]
  (let [[props children] (u/parse-props-children props children)]
    (into [user-only-page (merge {:user-role-pred :user/employer?} props)] children)))

(defn subheader [title]
  [ui/subheader {:style styles/subheader} title])

(defn elegant-line [label body]
  [:div
   [:h3 {:style (merge styles/user-detail-h2-line
                       {:font-size "1.08em"})} label]
   [:h2 {:style (merge styles/user-detail-h2-line
                       {:font-size "1.22em"})} body]])

(defn call-on-change [{:keys [:args :load-on-mount?]}]
  (let [prev-args (r/atom (when-not load-on-mount? args))]
    (fn [{:keys [:on-change :args]} & childen]
      (when-not (= @prev-args args)
        (reset! prev-args args)
        (when (fn? on-change)
          (on-change args)))
      (into [:div] childen))))

(defn only-registered [& children]
  (if @(subscribe [:db/active-address-registered?])
    (into [:div] children)
    (when-not @(subscribe [:db/my-users-loading?])
      [centered-rows
       "You must register your address first"
       [ui/raised-button
        {:primary true
         :href (u/path-for :freelancer/create)
         :label "Become Freelancer"
         :style styles/margin-top-gutter-less}]
       [ui/raised-button
        {:primary true
         :href (u/path-for :employer/create)
         :label "Become Employer"
         :style styles/margin-top-gutter-less}]])))

(defn not-connected-body []
  [centered-rows
   "You have no accounts connected. Please see How it works section for more information"
   [ui/raised-button
    {:primary true
     :href (u/path-for :how-it-works)
     :label "How it works"
     :style styles/margin-top-gutter-less}]])

(defn only-connected [& children]
  (if @(subscribe [:db/active-address])
    (into [:div] children)
    [not-connected-body]))

(defn only-unregistered-and-connected [& children]
  (if @(subscribe [:db/active-address-registered?])
    [centered-rows
     "This address was already registered. See your profile for making changes"
     [ui/raised-button
      {:primary true
       :href (u/path-for :user/edit)
       :label "My Profile"
       :style styles/margin-top-gutter-less}]]
    (into [only-connected] children)))

(defn long-text [props & children]
  (let [[props children] (u/parse-props-children props children)]
    [:div
     (r/merge-props
       {:style (merge styles/allow-whitespaces
                      styles/word-wrap-break)}
       props)
     (into [linkify] children)]))

(defn search-filter-reset-button []
  [row-plain
   {:center "xs"}
   [ui/flat-button
    {:style (merge styles/margin-top-gutter-less
                   styles/full-width)
     :primary true
     :label "Reset"
     :on-touch-tap #(dispatch [:location/set-query nil])}]])

(defn search-filter-done-button []
  (let [xs-sm-width? (subscribe [:window/xs-sm-width?])]
    (fn [props]
      (when @xs-sm-width?
        [row-plain
         {:center "xs"}
         [ui/flat-button
          (merge
            {:style (merge styles/margin-top-gutter-less
                           styles/full-width)
             :primary true
             :label "Done"}
            props)]]))))

(defn logo [props]
  [:a
   (r/merge-props
     {:href (u/path-for :home)
      :style styles/ethlance-logo}
     props)
   "Ethlance"])

(defn currency [value opts]
  [:span {:title (str (u/big-num->num value) "Ξ")}
   @(subscribe [:selected-currency/converted-value value opts])])

(defn rate [rate payment-type opts]
  (when rate
    [:span
     [currency rate opts]
     (when (= 1 payment-type)
       " / hr")]))

(defn add-more-skills-button []
  [ui/raised-button
   {:label "Add more skills"
    :primary true
    :href (u/path-for :skills/create)
    :icon (icons/plus)}])

(def privacy-warning-hint
  "Note, all communication on Ethlance is unencrypted on public blockchain. Please don't reveal any private information.")

(defn how-it-works-app-bar-link [props & children]
  (let [[props children] (u/parse-props-children props children)]
    [:div
     (r/merge-props
      {:style {:margin-top 15 :text-align :right}}
      props)
     [:a
      {:href (u/path-for :how-it-works)}
      [:h3.bolder
       {:style styles/app-bar-link}
       (or (first children) "How it works")]]]))


(defn mobile-coinbase-app-bar-link []
  (let [android? @(subscribe [::mobile-subs/android?])
        ios? @(subscribe [::mobile-subs/ios?])
        coinbase-mobile-store-link
        (cond
          (true? android?) (:android-mobile-link constants/coinbase)
          (true? ios?) (:ios-mobile-link constants/coinbase)
          :else (:main-mobile-link constants/coinbase))]
    (fn []
      [:a {:href coinbase-mobile-store-link}
        [:div {:style (merge styles/app-bar-link
                             {:display "flex" :align-items "center"})}
         [:span {:style {:color "white"
                         :height "2em"
                         :line-height "2em"}} "Pay with"]
         [:img {:style {:height "2em"
                        :width "103.7px"
                        :background-color "white"
                        :border-radius "3px"
                        :margin-left "0.7em"
                        :margin-right "0.7em"}
                :src "/images/coinbase-logo.png"}]]])))


(defn- link [href text]
  [:a {:href href
       :target :_blank}
   text])

(defn connect-button-layout []
  (let [xs-width? (subscribe [:window/xs-width?])]
    (fn [{:keys [:connect-button-props :connected? :clear-button-props]} text-field]
      [row-plain
       {:bottom "xs"}
       text-field
       [:div
        {:style (if @xs-width? (merge styles/full-width
                                      {:float :left})
                               {})}
        (let [btn-style (merge {:margin-bottom 10}
                               (when-not @xs-width?
                                 {:margin-left 10}))]
          (if connected?
            [ui/raised-button
             (r/merge-props
               {:label "Clear"
                :primary true
                :style btn-style}
               clear-button-props)]
            [ui/raised-button
             (r/merge-props
               {:label "Connect"
                :primary true
                :label-position "before"
                :style btn-style}
               connect-button-props)]))]])))

(defn conversion-rate [{:keys [:currency :value]}]
  [:span "1 ETH = " (u/format-currency value currency {:full-length? true :display-code? true})])

(defn youtube [props]
  [:iframe
   (r/merge-props
     props
     {:width 560
      :height 315
      :frameBorder 0
      :allowFullScreen true})])
