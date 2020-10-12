(ns ethlance.ui.page.devcard
  "Development Page for showing off different reagent components"
  (:require [district.ui.component.page :refer [page]]
            [ethlance.shared.constants :as constants]
            [ethlance.ui.component.button :refer [c-button c-button-label]]
            [ethlance.ui.component.checkbox :refer [c-labeled-checkbox]]
            [ethlance.ui.component.circle-button :refer [c-circle-icon-button]]
            [ethlance.ui.component.ethlance-logo :refer [c-ethlance-logo]]
            [ethlance.ui.component.icon :refer [c-icon]]
            [ethlance.ui.component.inline-svg :refer [c-inline-svg]]
            [ethlance.ui.component.rating :refer [c-rating]]
            [ethlance.ui.component.scrollable :refer [c-scrollable]]
            [ethlance.ui.component.search-input :refer [c-chip-search-input]]
            [ethlance.ui.component.select-input :refer [c-select-input]]))

(defmethod page :route.devcard/index []
  (fn []
    [:div.page-devcard
     [:div.header [:b "Ethlance Components"]]
     [:div.spacer]
     [:div.sidebar "nothing here"]
     [:div.content
      [:div.grouping
       [:div.title "Ethlance Logo"]
       [:div.body
        [c-ethlance-logo {:color :primary}]
        [c-ethlance-logo {:color :secondary}]]]

      [:div.grouping
       [:div.title "Ethlance Button"]
       [:div.body
        [c-button {:on-click (fn [] (println "Test"))}
         [c-button-label {}
          [:span "Become a " [:b "Freelancer"]]]]

        [c-button {:color :primary
                   :on-click (fn [] (println "Test"))}
         [c-button-label {}
          [:span "Become an " [:b "Employer"]]]]

        [c-button {:color :secondary
                   :on-click (fn [] (println "Test"))}
         [c-button-label {}
          [:span "Become a " [:b "Freelancer"]]]]

        [c-button {:color :secondary
                   :on-click (fn [] (println "Test"))}
         [c-button-label {}
          [:span "Become an " [:b "Employer"]]]]

        [c-button {:color :primary
                   :disabled? true
                   :on-click (fn [] (println "Test"))}
         [c-button-label {}
          [:span "Become an " [:b "Employer"]]]]

        [c-button {:color :secondary
                   :disabled? true
                   :on-click (fn [] (println "Test"))}
         [c-button-label {}
          [:span "Become an " [:b "Employer"]]]]

        [c-button {:color :primary
                   :active? true
                   :on-click (fn [] (println "Test"))}
         [c-button-label {}
          [:span "Become an " [:b "Employer"]]]]

        [c-button {:color :secondary
                   :active? true
                   :on-click (fn [] (println "Test"))}
         [c-button-label {}
          [:span "Become an " [:b "Employer"]]]]

        [c-button {:color :primary
                   :size :large
                   :on-click (fn [] (println "Test"))}
         [c-button-label {}
          [:span "Become an " [:b "Employer"]]]]

        [c-button {:color :secondary
                   :size :small
                   :on-click (fn [] (println "Test"))}
         [c-button-label {} [:span "Freelancer"]]]]]

      [:div.grouping
       [:div.title "Ethlance Inline SVG"]
       [:div.body
        [c-inline-svg {:src "images/ethlance_logo_secondary.svg"
                       :id "specific-logo"
                       :class "ethlance-logo-svg"
                       :on-ready (fn []
                                   (.log js/console "SVG Ready"))}]

        [c-button {:color :primary
                   :size :small
                   :on-click (fn [] (println "Test"))}
         [c-button-label {} [c-icon {:color :none :size :normal}]]]]]

      [:div.grouping
       [:div.title "Ethlance Circle Button with Icon"]
       [:div.body

        [c-circle-icon-button {}]

        [c-circle-icon-button {:color :secondary}]

        [c-circle-icon-button {:name :about}]

        [c-circle-icon-button {:name :arbiters}]
        [c-circle-icon-button {:name :candidates}]
        [c-circle-icon-button {:name :jobs}]

        [c-circle-icon-button {:name :search}]
        [c-circle-icon-button {:name :sign-up}]

        [c-circle-icon-button {:name :facebook}]
        [c-circle-icon-button {:name :github}]
        [c-circle-icon-button {:name :linkedin}]
        [c-circle-icon-button {:name :slack}]
        [c-circle-icon-button {:name :twitter}]]]

      [:div.dark-grouping
       [:div.title "Ethlance Circle Button with Icon (dark)"]
       [:div.body
        [c-circle-icon-button {:name :about :color :secondary}]

        [c-circle-icon-button {:name :arbiters :color :secondary}]
        [c-circle-icon-button {:name :candidates :color :secondary}]
        [c-circle-icon-button {:name :jobs :color :secondary}]

        [c-circle-icon-button {:name :search :color :secondary}]
        [c-circle-icon-button {:name :sign-up :color :secondary}]

        [c-circle-icon-button {:name :facebook :color :secondary}]
        [c-circle-icon-button {:name :github :color :secondary}]
        [c-circle-icon-button {:name :linkedin :color :secondary}]
        [c-circle-icon-button {:name :slack :color :secondary}]
        [c-circle-icon-button {:name :slack :color :none}]
        [c-circle-icon-button {:name :twitter :color :secondary}]]]

      [:div.grouping
       [:div.title "Ethlance Rating"]
       [:div.body
        [c-rating {:default-rating 3 :on-change (fn [index] (println (str "Rating: " index)))}]
        [c-rating {:color :white :default-rating 1}]
        [c-rating {:color :black :default-rating 2}]]]

      [:div.grouping
       [:div.title "Ethlance Icons"]
       [:div.body
        [c-icon {:name :about :color :black}]
        [c-icon {:name :arbiters :color :black}]
        [c-icon {:name :candidates :color :black}]
        [c-icon {:name :close :color :black}]
        [c-icon {:name :facebook :color :black}]
        [c-icon {:name :github :color :black}]
        [c-icon {:name :ic-arrow-up :color :black}]
        [c-icon {:name :ic-arrow-down :color :black}]
        [c-icon {:name :ic-arrow-left :color :black}]
        [c-icon {:name :ic-arrow-left2 :color :black}]
        [c-icon {:name :ic-arrow-right :color :black}]
        [c-icon {:name :ic-arrow-right2 :color :black}]
        [c-icon {:name :ic-upload :color :black}]]

       [:div.body
        [c-icon {:name :about :color :secondary}]
        [c-icon {:name :arbiters :color :secondary}]
        [c-icon {:name :candidates :color :secondary}]
        [c-icon {:name :close :color :secondary}]
        [c-icon {:name :facebook :color :secondary}]
        [c-icon {:name :github :color :secondary}]
        [c-icon {:name :ic-arrow-up :color :secondary}]
        [c-icon {:name :ic-arrow-down :color :secondary}]
        [c-icon {:name :ic-arrow-left :color :secondary}]
        [c-icon {:name :ic-arrow-left2 :color :secondary}]
        [c-icon {:name :ic-arrow-right :color :secondary}]
        [c-icon {:name :ic-arrow-right2 :color :secondary}]
        [c-icon {:name :ic-upload :color :secondary}]]]

      [:div.grouping
       [:div.title "Ethlance Selection Input (light)"]
       [:div.body
        [c-select-input {:label "Select Country"
                         :selections #{"United States" "Canada" "Germany" "Australia"}
                         :style {:width 200}}]

        [c-select-input {:label "Select Country"
                         :selections (sort #{"United States" "Canada" "Germany" "Australia" "Mexico" "France"})
                         :search-bar? true
                         :style {:width 200}}]]]

      [:div.dark-grouping
       [:div.title "Ethlance Selection Input (dark)"]
       [:div.body
        [c-select-input {:label "Select Country"
                         :selections #{"United States" "Canada" "Germany" "Australia"}
                         :color :secondary
                         :style {:width 200}}]

        [c-select-input {:label "Select Country"
                         :selections (sort #{"United States" "Canada" "Germany" "Australia" "Mexico" "France"})
                         :color :secondary
                         :search-bar? true
                         :style {:width 200}}]]]


      [:div.grouping
       [:div.title "Ethlance Chip Search Input (light)"]
       [:div.body
        [c-chip-search-input {:default-chip-listing #{"C++" "Python"}
                              :auto-suggestion-listing ["Clojure" "Clojurescript"]}]
        [c-chip-search-input {:default-chip-listing #{"Canada"}
                              :auto-suggestion-listing constants/countries
                              :placeholder "Countries Visited"
                              :search-icon? false}]]]

      [:div.grouping
       [:div.title "Ethlance Checkbox Input (light)"]
       [:div.body
        [c-labeled-checkbox {:default-checked? false
                             :label "Testing"}]]]

      [:div.grouping
       [:div.title "Ethlance Scrollable (light)"]
       [:div.body
        [:div.scrollable-fixed-vertical
         [c-scrollable {:forceVisible true :autoHide false}
          [:ul
           (doall
            (for [i (range 30)]
              ^{:key (str "el-" i)}
              [:li (str "Element " (inc i))]))]]]

        [:div.scrollable-fixed-horizontal
         [c-scrollable {:forceVisible true :autoHide false}
          [:div
           (doall
            (for [i (range 30)]
              ^{:key (str "el-" i)}
              [:span (str "Element " (inc i))]))]]]]]

      [:div.dark-grouping
       [:div.title "Ethlance Scrollable (dark)"]
       [:div.body
        [:div.scrollable-fixed-vertical
         [c-scrollable {:maxHeight 400 :forceVisible true :autoHide false}
          [:ul
           (doall
            (for [i (range 30)]
              [:li (str "Element " (inc i))]))]]]

        [:div.scrollable-fixed-horizontal
         [c-scrollable {}
          [:div
           (doall
            (for [i (range 30)]
              [:span (str "Element " (inc i))]))]]]]]]]))
