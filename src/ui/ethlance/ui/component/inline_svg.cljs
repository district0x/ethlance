(ns ethlance.ui.component.inline-svg
  "Reagent component that correctly inlines an SVG image such that it
  exposes the SVG elements to the HTML DOM. This allows the SVG to be
  manipulated using the javascript DOM api, and allows CSS styling.

  # References

  - https://stackoverflow.com/questions/24933430/img-src-svg-changing-the-fill-color
  "
  (:require
   [reagent.core :as r]))


(defn fetch-url
  "Returns js/Promise"
  [url]
  (.fetch js/window url))


(defn parse-xml-from-string
  [s]
  (let [parser (js/DOMParser.)]
    (.parseFromString parser s "text/xml")))
    

(defn xml->svg
  [xml]
  (let [svg (-> xml (.getElementsByTagName "svg") (aget 0))]
    (doto svg
      (.removeAttribute "xmlns:a"))))

    ;; Set the viewport


(defn prepare-svg
  [url]
  (-> (fetch-url url)
      (.then (fn [response] (.text response)))
      (.then (fn [text]
               (-> text
                   parse-xml-from-string
                   xml->svg)))))


(defn c-inline-svg
  [{:keys [key src class id on-ready width height] :as props}]
  (let [*inline-svg (r/atom nil)
        *dom-ref (r/atom nil)]
    (r/create-class
     {:display-name "c-inline-svg"

      :component-did-mount
      (fn [this]
        (-> (prepare-svg src)
            (.then (fn [svg] (reset! *inline-svg svg)))))

      :component-did-update
      (fn [this _]
        (let [{:keys [key id class width height on-ready]}
              (-> this r/argv second)]
          (when (and @*dom-ref @*inline-svg)
            (let [inline-svg @*inline-svg]
              (when id (.setAttribute inline-svg "id" id))
              (when class (.setAttribute inline-svg "class" class))
              (when width (.setAttribute inline-svg "width" width))
              (when height (.setAttribute inline-svg "height" height))
              (when-not (= @*inline-svg (-> @*dom-ref .-firstChild))
                (doto @*dom-ref
                  (aset "innerHTML" "")
                  (.appendChild inline-svg)))
              (when on-ready
                (on-ready @*dom-ref @*inline-svg))))))

      :reagent-render
      (fn [{:keys [key src class id width height on-ready] :as props}]
        (let [style (cond-> {}
                      width (assoc :width width)
                      height (assoc :height height))]
          [:div.ethlance-inline-svg
           {:ref (fn [com] (reset! *dom-ref com)) :key key}
           (when-not @*inline-svg [:img.svg {:src src :style style}])]))})))
