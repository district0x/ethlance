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
  (when-let [svg (-> xml (.getElementsByTagName "svg") (aget 0))]
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
      (fn [this old-argv]
        (let [{:keys [key id root-class class width height on-ready src]}
              (-> this r/argv second)
              old-src (-> old-argv second :src)]

          ;; To support changing the src of an inline-svg, need to
          ;; kickstart retrieving the new src and clear out the old
          ;; one
          (when-not (= src old-src)
            (reset! *inline-svg nil)
            (-> (prepare-svg src)
                (.then (fn [svg] (reset! *inline-svg svg)))))

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
      (fn [{:keys [key src class id root-class width height on-ready] :as props}]
        (let [style (cond-> {}
                      width (assoc :width (str width "px"))
                      height (assoc :height (str height "px")))]
          [:div.ethlance-inline-svg
           {:class root-class
            :ref (fn [com] (reset! *dom-ref com))
            :key key}
           (when-not @*inline-svg #_[:img.svg {:src src :style style :class class}])]))})))
