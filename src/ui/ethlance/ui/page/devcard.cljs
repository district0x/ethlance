(ns ethlance.ui.page.devcard
  "Development Page for showing off different reagent components"
  (:require
   [district.ui.component.page :refer [page]]

   ;; Ethlance Components
   [ethlance.ui.component.ethlance-logo :refer [c-ethlance-logo]]
   [ethlance.ui.component.button :refer [c-button c-button-label]]
   [ethlance.ui.component.inline-svg :refer [c-inline-svg]]
   [ethlance.ui.component.icon :refer [c-icon]]
   [ethlance.ui.component.circle-button :refer [c-circle-icon-button]]
   [ethlance.ui.component.rating :refer [c-rating]]))


(defmethod page :route.devcard/index []
  (let []
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
          [c-button {:on-click (fn [e] (println "Test"))}
           [c-button-label {}
            [:span "Become a " [:b "Freelancer"]]]]

          [c-button {:color :primary
                     :on-click (fn [e] (println "Test"))}
           [c-button-label {}
            [:span "Become an " [:b "Employer"]]]]

          [c-button {:color :secondary
                     :on-click (fn [e] (println "Test"))}
           [c-button-label {}
            [:span "Become a " [:b "Freelancer"]]]]

          [c-button {:color :secondary
                     :on-click (fn [e] (println "Test"))}
           [c-button-label {}
            [:span "Become an " [:b "Employer"]]]]

          [c-button {:color :primary
                     :disabled? true
                     :on-click (fn [e] (println "Test"))}
           [c-button-label {}
            [:span "Become an " [:b "Employer"]]]]

          [c-button {:color :secondary
                     :disabled? true
                     :on-click (fn [e] (println "Test"))}
           [c-button-label {}
            [:span "Become an " [:b "Employer"]]]]

          [c-button {:color :primary
                     :active? true
                     :on-click (fn [e] (println "Test"))}
           [c-button-label {}
            [:span "Become an " [:b "Employer"]]]]

          [c-button {:color :secondary
                     :active? true
                     :on-click (fn [e] (println "Test"))}
           [c-button-label {}
            [:span "Become an " [:b "Employer"]]]]

          [c-button {:color :primary
                     :size :large
                     :on-click (fn [e] (println "Test"))}
           [c-button-label {}
            [:span "Become an " [:b "Employer"]]]]

          [c-button {:color :secondary
                     :size :small
                     :on-click (fn [e] (println "Test"))}
           [c-button-label {} [:span "Freelancer"]]]]]

        [:div.grouping
         [:div.title "Ethlance Inline SVG"]
         [:div.body
          [c-inline-svg {:src "images/ethlance_logo_secondary.svg"
                         :id "specific-logo"
                         :class "ethlance-logo-svg"
                         :on-ready (fn [dom svg]
                                     (.log js/console "SVG Ready"))}]

          [c-button {:color :primary
                     :size :small
                     :on-click (fn [e] (println "Test"))}
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
          [c-circle-icon-button {:name :twitter}]

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
          [c-rating {:rating 3 :on-change (fn [index] (println (str "Rating: " index)))}]
          [c-rating {:color :white :rating 1}]
          [c-rating {:color :black :rating 2}]]]]])))

