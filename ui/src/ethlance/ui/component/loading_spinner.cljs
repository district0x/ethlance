(ns ethlance.ui.component.loading-spinner)


(defn c-loading-spinner
  []
  [:div.loading-spinner
   [:img {:src "/images/svg/ethlance_spinner.svg"}]])

(defn c-spinner-until-data-ready
  [loading-states component-when-loading-finished]
  (if (not-every? false? loading-states)
    [c-loading-spinner]
    component-when-loading-finished))
