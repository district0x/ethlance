(ns ethlance.ui.component.ethlance-logo
  "The ethlance logo as an SVG image")


(def primary-logo-url "images/ethlance_logo_primary.svg")
(def secondary-logo-url "images/ethlance_logo_secondary.svg")
(def white-logo-url "images/ethlance_logo_white.svg")
(def black-logo-url "images/ethlance_logo_bw.svg")


(defn c-ethlance-logo []
  (fn [{:keys [color] :or {color :primary}}]
    (let [src (case color
               :primary primary-logo-url
               :secondary secondary-logo-url
               :white white-logo-url
               :black black-logo-url)]
      [:div.ethlance-logo
       [:img {:src src :class []}]])))
