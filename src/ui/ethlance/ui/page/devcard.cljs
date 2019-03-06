(ns ethlance.ui.page.devcard
  "Development Page for showing off different reagent components"
  (:require
   [district.ui.component.page :refer [page]]

   ;; Ethlance Components
   [ethlance.ui.component.ethlance-logo :refer [c-ethlance-logo]]
   [ethlance.ui.component.button :refer [c-button c-button-label]]
   [ethlance.ui.component.inline-svg :refer [c-inline-svg]]
   [ethlance.ui.component.icon :refer [c-icon]]
   [ethlance.ui.component.circle-button :refer [c-circle-icon-button]]))


(defmethod page :route.devcard/index []
  (let []
    (fn []
      [:div.page-devcard
       [:div.devcard-header
        [:b "Ethlance Components"]]
       [c-ethlance-logo {:color :primary}]
       [c-ethlance-logo {:color :secondary}]
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
        [c-button-label {} [:span "Freelancer"]]]

  
       [c-inline-svg {:src "images/ethlance_logo_secondary.svg"
                      :id "specific-logo"
                      :class "ethlance-logo-svg"
                      :on-ready (fn [dom svg]
                                  (.log js/console "SVG Ready")
                                  (.log js/console dom svg))}]

       [c-button {:color :primary
                  :size :small
                  :on-click (fn [e] (println "Test"))}
        [c-button-label {} [c-icon {:color :none :size :normal}]]]

       [c-circle-icon-button {}]

       [c-circle-icon-button {:color :secondary}]])))
