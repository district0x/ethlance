(ns ethlance.ui.component.scrollable
  (:require
   [reagent.core :as r]
   ["react" :as react]
   ["simplebar" :as simplebar]))


(defn- c-scrollable-noop [_opts body] body)
(defn- c-scrollable-real
  "Scrollable container. Uses simplebar-react

  # Keyword Options (opts)

  :forceVisible - If true, will always show the scrollbar

  # Notes

  - Additional Readme Options (opts)
    https://github.com/Grsmto/simplebar/blob/master/packages/simplebar/README.md#options"
  [opts _]
  (let [*instance (r/atom nil)
        react-ref (react/createRef)]
    (r/create-class
     {:display-name "c-scrollable"

      :component-did-mount
      (fn [this]
        (let [elnode (.-current react-ref)
              simplebar (simplebar elnode (clj->js opts))]
          (reset! *instance simplebar)))

      :component-will-unmount
      (fn []
        (.unMount @*instance))

      :reagent-render
      (fn [_ child]
        [:div.scrollable {:ref react-ref} child])})))

(defn c-scrollable
  [opts val]
  (let [use-noop true] ; Using noop implementation until can make the simplebar work
    (if use-noop
      (c-scrollable-noop opts val)
      (c-scrollable-real opts val))))
