(ns ethlance.ui.component.scrollable
  (:require
   [reagent.core :as r]
   [simplebar-react]))


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
  [:> simplebar-react
   (merge default-opts opts)
   child])
