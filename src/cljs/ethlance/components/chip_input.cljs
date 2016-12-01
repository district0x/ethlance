(ns ethlance.components.chip-input
  (:require [cljsjs.material-ui-chip-input]
            [reagent.core :as r]))

(def chip-input (r/adapt-react-class js/MaterialUIChipInput))


