(ns ethlance.components.list-pagination
  (:require
    [cljs-react-material-ui.icons :as icons]
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.pagination :refer [pagination]]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn list-pagination [{:keys [all-subscribe]}]
  (let [all-ids (subscribe all-subscribe)]
    (fn [{:keys [offset limit load-dispatch list-db-path]}]
      (when (pos? (count @all-ids))
        [pagination
         {:current-page (inc (/ offset limit))
          :total-pages (js/Math.ceil (/ (count @all-ids) limit))
          :on-change (fn [page]
                       (let [offset (* (max (dec page) 0) limit)]
                         (dispatch [:list/set-offset list-db-path offset])
                         (dispatch (conj load-dispatch (u/paginate offset limit @all-ids)))))}]))))


