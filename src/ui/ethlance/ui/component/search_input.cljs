(ns ethlance.ui.component.search-input
  (:require
   [clojure.core.async :as async :refer [go go-loop <! >! chan close! put! timeout] :include-macros true]
   [reagent.core :as r]
   [taoensso.timbre :as log]
   
   ;; Ethlance Components
   [ethlance.ui.component.icon :refer [c-icon]]))


(defn c-chip
  [{:keys [on-close]} label]
  [:div.ethlance-chip
   [:span.label label]
   [:span.close-button {:on-click on-close}
    [c-icon {:name :close :size :x-small}]]])


(defn c-chip-search-input
  [{:keys [default-chip-listing auto-suggestion-listing on-chip-listing-change]}]
  (let [*chip-listing (r/atom (or (set default-chip-listing) #{}))
        *dom-search-input-ref (r/atom nil)]
    (r/create-class
     {:display-name "ethlance-chip-search-input"
      :component-did-mount
      (fn [this]
        )

      :component-will-unmount
      (fn [this]
        )

      :reagent-render
      (fn []
        [:div.ethlance-chip-search-input {:key "chip-search-input"}
         [:div.search-container
          [:div.chip-listing
           (doall
            (for [chip-label @*chip-listing]
              ^{:key (str "chip-" chip-label)}
              [c-chip 
               {:on-close #(swap! *chip-listing disj chip-label)}
               chip-label]))]
          [:input.search-input
           {:type "text"
            :on-key-down
            (fn [event]
              (cond
                (= (aget event "key") "Enter")
                (do
                  (swap! *chip-listing conj (aget event "target" "value"))
                  (aset event "target" "value" ""))))
            :ref #(reset! *dom-search-input-ref %)
            :placeholder "Search Tags"}]]
         [:div.search-button [c-icon {:name :search :size :normal}]]])})))

