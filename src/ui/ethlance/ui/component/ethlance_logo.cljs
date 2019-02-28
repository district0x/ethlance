(ns ethlance.ui.component.ethlance-logo
  "The ethlance logo as an SVG image")


(defn c-ethlance-logo []
  (fn [{:keys [] :as props}]
    [:div.ethlance-logo
     [:img (merge {:src "images/ethlance_logo.svg"} props)]]))
