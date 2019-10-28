(ns ethlance.ui.component.scrollable
  (:require
   [reagent.core :as r]))


(defn c-scrollable
  [opts child]
  (let [*mouse-down? (r/atom false)
        *current-position-x (r/atom 0)
        *current-position-y (r/atom 0)
        *scroll-position-x (r/atom 0)
        *scroll-position-y (r/atom 0)
        *scroll-delta-x (r/atom 0)
        *scroll-delta-y (r/atom 0)]
    (r/create-class
     {:display-name "c-scrollable"

      :component-did-mount
      (fn [this]
        (let [dom-scrollable (r/dom-node this)
              dom-container (.querySelector dom-scrollable ".container")
              dom-scroll-x (.querySelector dom-scrollable ".scroll-bar-x")
              dom-scroll-y (.querySelector dom-scrollable ".scroll-bar-y")]

          ;; Container Events
          (.addEventListener dom-container "mousedown" #(reset! *mouse-down? true))
          (.addEventListener dom-container "mouseup" #(reset! *mouse-down? false))
          (.addEventListener
           dom-container "mousemove"
           (fn [event]
             (let [mouse-x (aget event "clientX")
                   mouse-y (aget event "clientY")]
               (if-not @*mouse-down?
                 ;; Update the current position of the mouse while the mouse isn't pushed down
                 (do
                   (reset! *current-position-x mouse-x)
                   (reset! *current-position-y mouse-y))
                 (let [delta-x (- mouse-x @*current-position-x)
                       delta-y (- mouse-y @*current-position-y)
                       scroll-height (aget dom-container "scrollHeight")
                       scroll-width (aget dom-container "scrollWidth")]
                   (reset! *scroll-delta-x delta-x)
                   (reset! *scroll-delta-y delta-y)

                   (println "scroll height" scroll-height)
                   (println "scroll width" scroll-width)
                   (println "delta-x" delta-x)
                   (println "delta-y" delta-y)

                   ;; Clamp at [0, scroll-width]
                   (cond
                     (and (> delta-x 0) (> (+ delta-x @*scroll-position-x) scroll-width))
                     (reset! *scroll-position-x scroll-width)

                     (and (< delta-x 0) (< (+ delta-x @*scroll-position-x) 0))
                     (reset! *scroll-position-x 0)
                     
                     :else
                     (swap! *scroll-position-x #(+ % delta-x)))
                   
                   ;; Clamp at [0, scroll-height]
                   (cond
                     (and (> delta-y 0) (> (+ delta-y @*scroll-position-y) scroll-height))
                     (reset! *scroll-position-y scroll-height)

                     (and (< delta-y 0) (< (+ delta-y @*scroll-position-y) 0))
                     (reset! *scroll-position-y 0)
                     
                     :else
                     (swap! *scroll-position-y #(+ % delta-y))))))))))

      :reagent-render
      (fn [{:keys [fixed-width? min-width
                   fixed-height? min-height 
                   color]
            :or {fixed-width? true
                 min-width "100%"
                 fixed-height? true
                 min-height "100%"
                 color :primary}
            :as opts}
           child]
        ;; TODO: for dynamic situations, remove scrollable functionally when both are false instead.
        (assert (or fixed-width? fixed-height?) "At least `fixed-width? or fixed-height? must be true")
        (let [color-class (case color
                            :primary "primary"
                            :secondary "secondary"
                            "")]
          [:div.scrollable
           {:class [(when fixed-width? "fixed-width")
                    (when fixed-height? "fixed-height")
                    color-class]}
           [:div.container
            {:style {:min-width min-width
                     :min-height min-height
                     :left (str @*scroll-position-x "px")
                     :top (str @*scroll-position-y "px")}}
            child]
           [:div.scroll-bar-x]
           [:div.scroll-bar-y]]))})))


