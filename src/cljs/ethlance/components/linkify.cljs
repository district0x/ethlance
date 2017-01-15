(ns ethlance.components.linkify
  (:require [cljsjs.linkify-react]
            [reagent.core :as r]))

(def linkify (r/adapt-react-class js/Linkify))
