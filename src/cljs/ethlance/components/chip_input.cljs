(ns ethlance.components.chip-input
  (:require
    [cljs-react-material-ui.chip-input.reagent :as material-ui-chip-input]
    [ethlance.constants :as constants]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [medley.core :as medley]
    [reagent.core :as r]
    [cljs-react-material-ui.reagent :as ui]))

(defn chip-input [{:keys [:all-items :all-items-val-key :value :on-change :chip-backgroud-color] :as props}]
  (let [all-items-map? (map? all-items)
        data-source (if (map? all-items)
                      (u/map->data-source all-items all-items-val-key)
                      (u/coll->data-source (medley/indexed all-items)))]
    [material-ui-chip-input/chip-input
     (merge
       {:dataSource data-source
        :dataSourceConfig u/data-source-config
        :new-chip-key-codes []
        :full-width true
        :max-search-results 10}
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
       (dissoc props :value :on-change :all-items :all-items-val-key :chip-backgroud-color)
       {:on-request-delete (fn [item]
                             (let [items (into [] (remove (partial = item) value))]
                               (on-change items)))}
       {:on-request-add (fn [item]
                          (let [item (aget item "value")
                                items (conj (into [] value) item)]
                            (on-change items)))}
       (when value
         {:value
          (let [value (filter (partial >= (count all-items)) value)]
            (if all-items-map?
              (u/map->data-source (select-keys all-items value) all-items-val-key)
              (u/results-coll->data-source value all-items)))}))]))