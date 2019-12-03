(ns ethlance.ui.component.scrollable
  (:require
   [reagent.core :as r]
   [flib.simplebar]))


(defn c-scrollable [opts child]
  "Scrollable container. Uses simplebar-react

  # Keyword Options (opts)

  :forceVisible - If true, will always show the scrollbar

  # Notes

  - Additional Readme Options (opts)
    https://github.com/Grsmto/simplebar/blob/master/packages/simplebar/README.md#options"
  (let [*instance (r/atom nil)]
    (r/create-class
     {:display-name "c-scrollable"
      
      :component-did-mount
      (fn [this]
        (let [elnode (r/dom-node this)
              simplebar (js/SimpleBar. elnode (clj->js opts))]
          (reset! *instance simplebar)))

      :component-will-unmount
      (fn [this]
        (.unMount @*instance))

      :reagent-render
      (fn [opts child]
        [:div.scrollable child])})))
