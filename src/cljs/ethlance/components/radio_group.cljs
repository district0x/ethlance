(ns ethlance.components.radio-group
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.constants :as constants]
    [re-frame.core :refer [dispatch]]
    [reagent.core :as r]))

(defn radio-group [{:keys [:form-key :field-key :options] :as props}]
  [ui/radio-button-group
   (r/merge-props
     {:on-change #(dispatch [:form/set-value form-key field-key %2])}
     (dissoc props :form-key :field-key :options))
   (for [[key label] options]
     [ui/radio-button
      {:value key
       :key key
       :label label}])])
