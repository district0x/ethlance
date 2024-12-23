(ns ethlance.ui.component.scrollable
  (:require
    ["react" :as react]
    ["simplebar" :as simplebar]
    [reagent.core :as r]))


(defn- c-scrollable-noop
  [_opts body]
  body)


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
       (fn [_this]
         (let [elnode (.-current react-ref)
               simplebar (simplebar elnode (clj->js opts))]
           (reset! *instance simplebar)))

       :component-will-unmount
       (fn []
         (.unMount @*instance))

       :reagent-render
       (fn [_ child]
         [:div.scrollable {:ref react-ref} child])})))

(defn- c-scrollable-css
  [& [opts body]]
  [:div {:style {:overflow-x "auto"}} (or body opts)])

(defn c-scrollable
  [opts val]
  (let [alternative-implementation true] ; Using basic CSS implementation to make wide tables scroll horizontally
    (if alternative-implementation
      (c-scrollable-css opts val)
      (c-scrollable-real opts val))))
