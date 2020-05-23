(ns ethlance.ui.component.info-message
  (:require
   [reagent.core :as r]
   [taoensso.timbre :as log]))


(defn c-info-message
  "An info message component, displaying a info logo, along with an
  info message and a 'show details' dropdown for additional info."
  [message details]
  (let [*open? (r/atom false)]
    (log/info (str message " : " details))
    (fn []
      [:div.info-message
       [:div.logo
        [:img {:src "/images/svg/ethlance_spinner.svg"}]] ;; FIXME
       [:div.message message]
       (when details
         [:div.show-button
          {:on-click #(swap! *open? not)}
          (if @*open? "Hide Details" "Show Details")])
       (when @*open?
         [:div.details details])])))
