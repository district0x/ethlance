(ns ethlance.components.misc
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [cljs-web3.core :as web3]
    [clojure.string :as string]
    [ethlance.components.icons :as icons]
    [ethlance.components.linkify :refer [linkify]]
    [ethlance.components.list-pagination :refer [list-pagination]]
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
   (into [] (concat [col {:xs 12 :md 10 :lg 9 :style styles/text-left}]
                    children))])

(def text-field-base
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

(defn text-field []
  (let [eth-config (subscribe [:eth/config])]
    (fn [{:keys [:value :on-change :form-key :field-key :max-length-key :min-length-key] :as props}]
      (let [min-length (get @eth-config min-length-key 0)
            max-length (get @eth-config max-length-key)
            validator (if (and min-length max-length)
                        #(<= min-length (if (string? %) (count (string/trim %)) 0) max-length)
                        (constantly true))
            valid? (validator value)]
        [text-field-base
         (r/merge-props
           {:style styles/display-block
            :on-change #(dispatch [:form/set-value form-key field-key %2 validator])
            :error-text (when-not valid?
                          (if (pos? min-length)
                            (gstring/format "Write between %s and %s characters" min-length max-length)
                            "Text is too long"))}
           (dissoc props :form-key :field-key :max-length-key :form-key :field-key :min-length-key))]))))

(defn ether-field [{:keys [:value :on-change :form-key :field-key :on-change :allow-empty?] :as props}]
  [text-field-base
   (r/merge-props
     {:style styles/display-block
      :on-change (fn [e value]
                   (if on-change
                     (on-change value)
                     (dispatch [:form/set-value
                                form-key
                                field-key
                                value
                                #(u/non-neg-ether-value? % (select-keys props [:allow-empty?]))])))
      :error-text (when-not (u/non-neg-ether-value? value (select-keys props [:allow-empty?]))
                    "Invalid Ether value")}
     (dissoc props :form-key :field-key :on-change :allow-empty?))])

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

(defn blocked-user-chip []
  [status-chip
   {:background-color styles/danger-color}
   "This user has been blocked"])

(defn user-address [address]
  [:div
   {:style (merge styles/margin-bottom-gutter-less
                  styles/word-wrap-break)}
   [:a {:target :_blank
        :style {:color styles/primary1-color}
        :href (u/etherscan-url address)} address]])

(defn user-created-on [created-on]
  [:h4 {:style (merge styles/fade-text
                      {:margin-bottom 5})} "joined on " (u/format-date created-on)])

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

(defn only-unregistered [& children]
  (if @(subscribe [:db/active-address-registered?])
    [centered-rows
     "This address was already registered. See your profile for making changes"
     [ui/raised-button
      {:primary true
       :href (u/path-for :user/edit)
       :label "My Profile"
       :style styles/margin-top-gutter-less}]]
    (if (seq @(subscribe [:db/my-addresses]))
      (into [:div] children)
      [centered-rows
       "You have no accounts connected. Please see How it works section for more information"
       [ui/raised-button
        {:primary true
         :href (u/path-for :how-it-works)
         :label "How it works?"
         :style styles/margin-top-gutter-less}]])))

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

(defn search-paper-thin []
  (let [xs-sm-width? (subscribe [:window/xs-sm-width?])]
    (fn [& children]
      (into
        [paper-thin
         {:style (if @xs-sm-width? styles/no-box-shadow {})}]
        children))))

(defn search-results [{:keys [:items-count :loading? :offset :limit :no-items-found-text :no-more-items-text
                              :next-button-text :prev-button-text :on-page-change]} body]
  [paper-thin
   {:loading? loading?}
   (if (pos? items-count)
     body
     [row {:center "xs" :middle "xs"
           :style {:min-height 200}}
      (when-not loading?
        (if (zero? offset)
          [:div no-items-found-text]
          [:div no-more-items-text]))])
   [row-plain {:end "xs"}
    (when (pos? offset)
      [ui/flat-button
       {:secondary true
        :label prev-button-text
        :icon (icons/chevron-left)
        :on-touch-tap #(on-page-change (- offset limit))}])
    (when (= items-count limit)
      [ui/flat-button
       {:secondary true
        :label next-button-text
        :label-position :before
        :icon (icons/chevron-right)
        :on-touch-tap #(on-page-change (+ offset limit))}])]])

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
       {:style {:margin-right styles/desktop-gutter :margin-top 10}}
       props)
     [:a
      {:href (u/path-for :how-it-works)}
      [:h3.bolder
       {:style styles/white-text}
       (or (first children) "How it works?")]]]))

(defn- link [href text]
  [:a {:href href
       :target :_blank}
   text])