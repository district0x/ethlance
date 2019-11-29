(ns ethlance.ui.component.scrollable
  (:require
   [reagent.core :as r]
   [flib.simplebar]))


(def default-opts
  "Default options used within the scrollable component."
  {:classNames {:content "scrollable-content"
                :scrollContent "scrollable-scroll-content"
                :scrollbar "scrollable-scroll-bar"
                :track "scrollable-track"}
   :autoHide false
   :forceVisible true})


(defn c-scrollable [opts child]
  "Scrollable container. Uses simplebar-react

  # Keyword Options (opts)

  

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
