(ns ethlance.ui.component.tag
  "An ethlance tag component")


(defn c-tag
  "Tag Component for showing stylized tag information.

  # Keyword Arguments

  opts - React Props

  # Rest Arguments (children)

  Reagent components. Expects `c-tag-label`.

  # Examples

  [c-tag {} [c-tag-label \"C++ Programming\"]]"
  []
  (fn [opts & children]
    (into [:div.ethlance-tag opts] children)))


(defn c-tag-label
  "Tag Label to be used with the tag component."
  []
  (fn [label]
    [:span.ethlance-tag-label {:key (str label)} label]))
