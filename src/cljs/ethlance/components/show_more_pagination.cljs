(ns ethlance.components.show-more-pagination
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a]]
    [ethlance.components.pagination :refer [pagination]]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [re-frame.core :refer [subscribe dispatch dispatch-sync]]
    [reagent.core :as r]))

(defn show-more-pagination [{:keys [all-ids-subscribe offset initial-limit list-db-path]}]
  (let [all-ids (subscribe all-ids-subscribe)
        load-offset (r/atom initial-limit)]
    (dispatch-sync [:list/set-offset-limit list-db-path 0 initial-limit])
    (fn [{:keys [limit show-more-limit load-dispatch list-db-path load-per loading?]}]
      (let [all-ids-count (count @all-ids)]
        (when (and (not loading?)
                   (> all-ids-count limit))
          [row-plain
           {:style styles/full-width
            :center "xs"}
           [ui/flat-button
            {:secondary true
             :label "Show more"
             :on-touch-tap (fn []
                             (swap! load-offset + (dec limit))
                             (let [new-limit (+ @load-offset show-more-limit)]
                               (dispatch [:list/set-limit list-db-path new-limit])
                               (dispatch (into load-dispatch [(u/paginate @load-offset
                                                                          (- new-limit @load-offset)
                                                                          @all-ids) load-per]))))}]])))))