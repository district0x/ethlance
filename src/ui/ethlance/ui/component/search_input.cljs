(ns ethlance.ui.component.search-input
  (:require
   [clojure.core.async :as async :refer [go go-loop <! >! chan close! put! timeout] :include-macros true]
   [reagent.core :as r]
   [taoensso.timbre :as log]
   [cuerdas.core :as string]
   
   ;; Ethlance Components
   [ethlance.ui.component.icon :refer [c-icon]]))


(def blur-delay-ms 200) ;; ms
(def num-filter-selections 4)


(defn filter-selections
  [search-text selections]
  (if (and (not (empty? search-text)) (not (empty? selections)))
    (->> selections
         (filter #(string/includes? (string/lower %) (string/lower search-text)))
         (take num-filter-selections)
         vec)
    nil))


(defn next-element
  "Get the next element in `xs` after element `v`."
  [xs v]
  (let [index (.indexOf (or xs []) v)]
    (cond
      (< index 0) nil
      (>= (inc index) (count xs)) (first xs)
      :else (get xs (inc index)))))


(defn previous-element
  "Get the previous element in `xs` before element `v`."
  [xs v]
  (let [index (.indexOf (or xs []) v)]
    (cond
      (< index 0) nil
      (= index 0) (last xs)
      :else (get xs (dec index)))))


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
           allow-custom-chips?
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
      (fn [this]
        (add-watch *chip-listing :watcher
                   (fn [_ _ _ new-state]
                     (when on-chip-listing-change
                       (on-chip-listing-change new-state))))

        (let [root-dom (r/dom-node this)
              search-input (.querySelector root-dom ".search-input")]
          (.addEventListener
           search-input "blur"
           (fn []
             ;; Needs to be on a timeout for dropdown selections to work correctly.
             (.setTimeout 
              js/window
              (fn []
                (reset! *active-suggestion nil)
                (reset! *search-text ""))
              blur-delay-ms))
           true)))
      

      :component-will-unmount
      (fn [this]
        ;; Probably not necessary
        (remove-watch *chip-listing :watcher))
      

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
              (let [key (some-> (aget event "key") string/lower)]
                (case key
                  "enter"
                  (cond 
                    @*active-suggestion
                    (do
                      (swap! *chip-listing conj @*active-suggestion)
                      (reset! *search-text "")
                      (reset! *active-suggestion nil))
                    (and (> (count @*search-text) 0) allow-custom-chips?)
                    (do
                      (swap! *chip-listing conj @*search-text)
                      (reset! *search-text "")
                      (reset! *active-suggestion nil)))
                  
                  "arrowdown"
                  (let [suggestions (filter-selections @*search-text auto-suggestion-listing)]
                    (if @*active-suggestion
                      (let [next-active (next-element suggestions @*active-suggestion)]
                        (reset! *active-suggestion next-active))
                      (reset! *active-suggestion (first suggestions))))

                  "arrowup"
                  (let [suggestions (filter-selections @*search-text auto-suggestion-listing)]
                    (if @*active-suggestion
                      (let [previous-active (previous-element suggestions @*active-suggestion)]
                        (reset! *active-suggestion previous-active))
                      (reset! *active-suggestion (last suggestions))))

                  "escape"
                  (do
                    (reset! *active-suggestion nil)
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
                 {:class (when (= @*active-suggestion suggestion) "active")
                  :on-click (fn []
                              (swap! *chip-listing conj suggestion)
                              (reset! *search-text "")
                              (reset! *active-suggestion nil))}
                 suggestion]))]])])})))
