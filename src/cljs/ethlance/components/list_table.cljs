(ns ethlance.components.list-table
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a]]
    [ethlance.constants :as constants]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [medley.core :as medley]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn list-table [{:keys [list-subscribe]}]
  (let [list (subscribe list-subscribe)
        prev-initial-dispatch (r/atom nil)]
    (fn [{:keys [:title :initial-dispatch :header :body] :as props} & children]
      (let [{:keys [loading? items offset limit]} @list]
        (when-not (= @prev-initial-dispatch initial-dispatch)
          (reset! prev-initial-dispatch initial-dispatch)
          (dispatch (conj [:after-eth-contracts-loaded] [:list/load-ids initial-dispatch])))
        (into
          [paper
           {:loading? (or loading? (:loading? props))}
           (if (string? title)
             [:h2 title]
             title)
           (when header
             [header props @list])
           [body props @list]]
          children)))))
