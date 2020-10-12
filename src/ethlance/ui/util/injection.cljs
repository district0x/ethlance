(ns ethlance.ui.util.injection
  (:require [goog.functions :refer [throttle]]))

(def default-debounce-interval 200) ;; ms

(defn- handle-inject-data-scroll!
  "Injects window.scrollY as as datasource, which allows you to use css
  selectors to determine the current position on the page.

  Example:

  ;; CLJS
  (inject-data-scroll!)

  ;; CSS
  html:not([data-scroll='0']) {
    // Triggered when the page scrolls.
  }"
  [injection-selector]
  (fn []
    (let [elnode (.querySelector js/document injection-selector)
          scroll-y (aget elnode "scrollTop")]
      (aset elnode "dataset" "scroll" scroll-y))))

(defn inject-data-scroll!
  [{:keys [injection-selector debounce-interval]
    :or {injection-selector "#app"
         debounce-interval default-debounce-interval}}]
  (let [elnode (.querySelector js/document injection-selector)
        inject-function (handle-inject-data-scroll! injection-selector)
        debounce-function (throttle inject-function debounce-interval)]

    ;; Attach
    (.addEventListener elnode "scroll" debounce-function #js {:passive true})

    ;; Perform initial call
    (inject-function)))
