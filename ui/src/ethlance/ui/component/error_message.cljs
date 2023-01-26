(ns ethlance.ui.component.error-message
  (:require
   [reagent.core :as r]
   [taoensso.timbre :as log]))


(defn c-error-message
  "An error message component, displaying a error logo, along with an
  error message and a 'show details' dropdown for additional info."
  [message details]
  (let [*open? (r/atom false)]
    (log/error (str message " : " details))
    (fn []
      [:div.error-message
       [:div.logo
        [:img {:src "/images/svg/ethlance_spinner.svg"}]] ;; FIXME
       [:div.message message]
       (when details
         [:div.show-button
          {:on-click #(swap! *open? not)}
          (if @*open? "Hide Details" "Show Details")])
       (when @*open?
         [:div.details (str details)])])))
