(ns ethlance.components.truncated-text
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.layout :refer [col row paper]]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [reagent.core :as r]))

(def max-count 150)

(defn truncated-text []
  (let [open? (r/atom false)]
    (fn [text]
      (if @open?
        [:div text]
        [:div (u/truncate text max-count) [:span {:style styles/more-text
                                            :on-click #(reset! open? true)}
                                           (when (< max-count (count text))
                                             "More")]]))))


