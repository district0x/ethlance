(ns ethlance.ui.component.ethlance-logo
  "The ethlance logo as an SVG image"
  (:require
   [ethlance.ui.component.inline-svg :refer [c-inline-svg]]))


(def default-logo-url "/images/ethlance_logo.svg")
(def primary-logo-url "/images/ethlance_logo_primary.svg")
(def secondary-logo-url "/images/ethlance_logo_secondary.svg")
(def white-logo-url "/images/ethlance_logo_white.svg")
(def black-logo-url "/images/ethlance_logo_bw.svg")


(defn c-ethlance-logo 
  "Ethlance Logo Component which displays the ethlance logo.
  
  # Keyword Arguments
  
  opts - Optional Arguments

  # Optional Arguments (opts)

  :color - Choose between different color schemes of the ethlance
  logo, `:primary`, `:secondary`, `:white`, `:black`.

  "
  []
  (fn [{:keys [color inline?] :or {color :primary inline? true} :as opts}]
    (let [src (case color
                :primary primary-logo-url
                :secondary secondary-logo-url
                :white white-logo-url
                :black black-logo-url)
          opts (dissoc opts :color :inline?)]
      [:a.ethlance-logo
       opts
       (if inline?
         [c-inline-svg {:src primary-logo-url :color color}]
         [:img {:src src :class []}])])))
