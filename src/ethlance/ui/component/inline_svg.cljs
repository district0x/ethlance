(ns ethlance.ui.component.inline-svg
  "Reagent component that correctly inlines an SVG image such that it
  exposes the SVG elements to the HTML DOM. This allows the SVG to be
  manipulated using the javascript DOM api, and allows CSS styling.

  # References

  - https://stackoverflow.com/questions/24933430/img-src-svg-changing-the-fill-color
  "
  (:require
   [reagent.core :as r]
   [reagent.dom :as rdom]))

(def *cached-svg-listing (atom {}))

(defn fetch-url
  "Returns js/Promise"
  [url]
  (.fetch js/window url))

(defn parse-xml-from-string
  "Parses the given string of XML into a DOM structure. Used to parse
  the SVG to inline within the page."
  [s]
  (let [parser (js/DOMParser.)]
    (.parseFromString parser s "text/xml")))

(defn xml->svg
  "Removes namespacing from the given XML element to appear as an SVG
  element."
  [xml]
  (when-let [svg (-> xml (.getElementsByTagName "svg") (aget 0))]
    (doto svg
      (.removeAttribute "xmlns:a"))))

(defn- clone-element [elnode]
  (.cloneNode elnode true))

(defn- remove-element-children [elnode]
  (loop [child (aget elnode "lastElementChild")]
    (when child
      (.removeChild elnode child)
      (recur (aget elnode "lastElementChild"))))
  elnode)

(defn prepare-svg
  "Prepares the given SVG image residing at the given `url`.

  # Keyword Arguments

  url - The string url of the given SVG to be processed.

  # Return Value

  A DOMElement, consisting of the SVG DOM Structure.

  # Notes

  - Also performs caching for individual SVGs
  "
  [url]
  (if-let [cached-svg (get @*cached-svg-listing url)]
    (.resolve js/Promise (clone-element cached-svg))
    (-> (fetch-url url)
        (.then (fn [response] (.text response)))
        (.then (fn [text]
                 (let [svg (-> text parse-xml-from-string xml->svg)]
                   (swap! *cached-svg-listing assoc url svg)
                   (clone-element svg)))))))

(defn c-inline-svg
  [{:keys [src]}]
  (let [*inline-svg (r/atom nil)]
    (r/create-class
     {:display-name "c-inline-svg"

      :component-did-mount
      (fn []
        ;; Preemptive Caching
        (if-let [cached-svg (get @*cached-svg-listing src)]
          (reset! *inline-svg (clone-element cached-svg))
          (-> (prepare-svg src)
              (.then (fn [svg] (reset! *inline-svg svg))))))

      :component-did-update
      (fn [this old-argv]
        (let [{:keys [id class width height on-ready src]}
              (-> this r/argv second)
              old-src (-> old-argv second :src)]

          ;; To support changing the src of an inline-svg, need to
          ;; kickstart retrieving the new src and clear out the old
          ;; one
          (when-not (= src old-src)
            (-> (prepare-svg src)
                (.then (fn [svg] (reset! *inline-svg svg)))))

          (when @*inline-svg
            (let [inline-svg @*inline-svg
                  elnode (rdom/dom-node this)]
              (when id (.setAttribute inline-svg "id" id))
              (when class (.setAttribute inline-svg "class" class))
              (when width (.setAttribute inline-svg "width" width))
              (when height (.setAttribute inline-svg "height" height))
              (when (not= inline-svg (-> elnode .-firstChild))
                (doto elnode
                  (remove-element-children)
                  (.appendChild inline-svg)))
              (when on-ready
                (on-ready elnode inline-svg))))))

      :reagent-render
      (fn [{:keys [key class width height root-class]}]
        (let [style (cond-> {}
                      width (assoc :width (str width "px"))
                      height (assoc :height (str height "px")))]
          [:div.ethlance-inline-svg
           {:class root-class :key key}
           (when-not @*inline-svg [:img.svg {:style (merge style {:opacity 0})
                                             :class class}])]))})))
