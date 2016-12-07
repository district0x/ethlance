(ns ethlance.components.truncated-text
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.misc :refer [col row paper-thin]]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [reagent.core :as r]))

(defn truncated-text []
  (let [open? (r/atom false)]
    (fn [props & children]
      (let [[{:keys [more-text-props max-length] :or {max-length 200}} children]
            (u/parse-props-children props children)
            text (first children)]
        (if @open?
          [:div text]
          [:div (u/truncate text max-length) [:span (r/merge-props
                                                     {:style styles/more-text
                                                      :on-click #(reset! open? true)}
                                                     more-text-props)
                                             (when (< max-length (count text))
                                               "More")]])))))