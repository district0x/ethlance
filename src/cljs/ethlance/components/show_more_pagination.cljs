(ns ethlance.components.show-more-pagination
  (:require
    [cljs-react-material-ui.icons :as icons]
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a]]
    [ethlance.components.pagination :refer [pagination]]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn show-more-pagination [{:keys [all-subscribe]}]
  (let [all-ids (subscribe all-subscribe)]
    (fn [{:keys [offset limit show-more-limit load-dispatch list-db-path load-parts-count]}]
      (let [all-ids-count (count @all-ids)]
        (when (or (and (zero? offset)
                       (< all-ids-count limit))
                  (< all-ids-count (+ offset show-more-limit)))
          [row-plain
           {:style styles/full-width
            :center "xs"}
           [ui/flat-button
            {:primary true
             :label "Show more"
             :on-touch-tap (fn []
                             (let [offset (+ offset (if (zero? offset) limit show-more-limit))]
                               (dispatch [:list/set-offset list-db-path offset])
                               (dispatch (into load-dispatch [(u/paginate offset limit @all-ids) load-parts-count]))))}]])))))