(ns ethlance.components.checkbox-group
  (:require [cljs-react-material-ui.reagent :as ui]))

(defn on-check [key values on-change e checked?]
  (let [new-values (if checked?
                     (set (conj values key))
                     (remove #(= % key) values))]
    (on-change e new-values)))

(defn checkbox-group []
  (fn [{:keys [options values on-change]}]
    [:div
     (for [[key name] options]
       [ui/checkbox
        {:label name
         :key key
         :checked (contains? (set values) key)
         :on-check (partial on-check key values on-change)}])]))
