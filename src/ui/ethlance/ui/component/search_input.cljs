(ns ethlance.ui.component.search-input
  (:require
   [clojure.core.async :as async :refer [go go-loop <! >! chan close! put! timeout] :include-macros true]
   [reagent.core :as r]
   [taoensso.timbre :as log]
   [cuerdas.core :as string]
   
   ;; Ethlance Components
   [ethlance.ui.component.icon :refer [c-icon]]))


(defn filter-selections
  [search-text selections]
  (if (and (not (empty? search-text)) (not (empty? selections)))
    (->> selections
         (filter #(string/includes? (string/lower %) (string/lower search-text))))
    nil))


(defn c-chip
  [{:keys [on-close]} label]
  [:div.ethlance-chip
   [:span.label label]
   [:span.close-button
    {:on-click on-close}
    [c-icon {:name :close :size :x-small}]]])


(defn c-chip-search-input
  [{:keys [default-chip-listing
           auto-suggestion-listing
           on-chip-listing-change
           search-icon?
           placeholder]
    :or {search-icon? true
         placeholder "Search Tags"}}]
  (let [*active-suggestion (r/atom nil)
        *chip-listing (r/atom (or (set default-chip-listing) #{}))
        *search-text (r/atom "")]
    (r/create-class
     {:display-name "ethlance-chip-search-input"
      :component-did-mount
      (fn [this])
      

      :component-will-unmount
      (fn [this])
      

      :reagent-render
      (fn []
        [:div.ethlance-chip-search-input
         {:key "chip-search-input"
          :class (when-not search-icon? "no-search-icon")}
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
            :value @*search-text
            :on-change #(reset! *search-text (-> % .-target .-value))
            :on-key-down
            (fn [event]
              (let [key (aget event "key")]
                (case key
                  "Enter"
                  (do
                    (swap! *chip-listing conj @*search-text)
                    (reset! *search-text ""))
                  nil)))
            :placeholder placeholder}]]

         (when search-icon?
           [:div.search-button [c-icon {:name :search :size :normal}]])

         (when-let [suggestions (filter-selections @*search-text auto-suggestion-listing)]
           [:div.dropdown
            [:div.suggestion-listing
             (doall
              (for [suggestion suggestions]
                ^{:key (str "suggestion-" suggestion)}
                [:div.suggestion
                 {:on-click (fn []
                              (swap! *chip-listing conj suggestion)
                              (reset! *search-text "")
                              (reset! *active-suggestion nil))}
                 suggestion]))]])])})))
