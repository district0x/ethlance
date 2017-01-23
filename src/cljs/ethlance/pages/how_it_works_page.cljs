(ns ethlance.pages.how-it-works-page
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a]]))

(defn how-it-works-page []
  [misc/center-layout
   [paper
    [:h2 "How it works?"]
    ]])