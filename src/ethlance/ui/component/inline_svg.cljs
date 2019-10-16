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


(defn prepare-svg
  "Prepares the given SVG image residing at the given `url`.

  # Keyword Arguments

  url - The string url of the given SVG to be processed.

  # Return Value
  
  A DOMElement, consisting of the SVG DOM Structure.
  "
  [url]
  (-> (fetch-url url)
      (.then (fn [response] (.text response)))
      (.then (fn [text]
               (-> text
                   parse-xml-from-string
                   xml->svg)))))


(defn c-inline-svg
  "Inline SVG Component, so that an SVG element that exists as an SVG
  image can be processed and placed within the DOM. This allows CSS
  styling to be applied to SVGs that exist within the page.

  # Keyword Arguments

  props - Optional Arguments supplied to the inline svg component.

  # Optional Arguments (props)

  :key - Unique React key to distinguish the inline svg component

  :src - url source of the SVG image to inline.

  :class - class attribute to apply to the SVG image to use as a CSS selector.

  :id - id attribute to apply to the SVG image to use as a CSS selector.

  :on-ready - Called after the SVG element exists on the page. Given
  function receives two arguments (fn [dom-reference inline-svg])

  :width - Width of the SVG element on the page.

  :height - Height of the SVG element on the page.

  # Notes

  - When styling the SVG element using a CSS selector, given that most
  SVGs perform styling within the DOM directly, it is necessary to
  include the '!important' property to CSS styling in order to
  override this inline styling.

  - Some SVGs inadvertently set the opacity of elements to 0.0, making
  SVG elements transparent. If things aren't appearing, set opacity to
  1.0

  svg , <inner selector> {
    opacity: 1.0 !important;
  }

  - Using an SVG editor like inkscape, you can apply class attributes
  and id attributes to elements and groups contained within the
  SVG. This can be useful for styling and animating individual pieces
  of the SVG.
  "
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
