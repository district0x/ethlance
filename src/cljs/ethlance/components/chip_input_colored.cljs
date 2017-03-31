(ns ethlance.components.chip-input-colored
  (:require
    [cljs-react-material-ui.chip-input.reagent :as material-ui-chip-input]
    [ethlance.constants :as constants]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [medley.core :as medley]
    [reagent.core :as r]
    [cljs-react-material-ui.reagent :as ui]))

(defn chip-input-colored [{:keys [:chip-backgroud-color] :as props}]
  [material-ui-chip-input/chip-input
   (r/merge-props
     (when chip-backgroud-color
       {:chip-renderer (fn [obj key]
                         (let [{:keys [value text isFocused? isDisabled handleClick handleRequestDelete]}
                               (js->clj obj :keywordize-keys true)]
                           (r/as-element
                             [ui/chip
                              {:key key
                               :background-color (if isFocused?
                                                   (styles/emphasize chip-backgroud-color)
                                                   chip-backgroud-color)
                               :style {:margin "8px 8px 0 0"
                                       :float :left
                                       :pointerEvents (if isDisabled "none" nil)}
                               :on-touch-tap handleClick
                               :on-request-delete handleRequestDelete}
                              text])))})
     (dissoc props :chip-backgroud-color))])