(ns ethlance.ui.component.select-input
  (:require
   [cuerdas.core :as string]
   [reagent.core :as r]
   
   [ethlance.ui.component.icon :refer [c-icon]]))


(defn filter-selections
  [search-text selections]
  (if-not (empty? search-text)
    (->> selections
         (filter #(string/includes? (string/lower %) (string/lower search-text))))
    selections))


(defn c-select-input
  [{:keys [label selections on-select default-selection color search-bar? default-search-text]
    :or {default-search-text "Search"}
    :as opts}]
  (let [opts (dissoc opts :label :selections :on-select :default-selection :color :search-bar? :default-search-text)
        *open? (r/atom false)
        *current-selection (r/atom default-selection)
        *search-text (r/atom "")
        color (or color :primary)
        color-class (case color
                      :primary " primary "
                      :secondary " secondary ")
        icon-color (case color
                     :primary :black
                     :secondary :white)]
    (fn [{:keys [label selections on-select default-selection color] :as opts}]
      [:div.ethlance-select-input (merge {:class color-class} opts)
       [:div.main
        {:on-click #(swap! *open? not)}
        [:span.label (or @*current-selection label)]
        [c-icon {:class "icon"
                 :name (if @*open? :ic-arrow-down :ic-arrow-up)
                 :color icon-color
                 :size :small}]]
       (when @*open?
         [:div.dropdown
          (when search-bar?
            [:div.search-bar
             [:input {:type "text"
                      :on-change #(reset! *search-text (-> % .-target .-value))
                      :placeholder default-search-text
                      :value @*search-text}]
             [c-icon {:name :close :size :x-small :title "Clear Search" :on-click #(reset! *search-text "")}]])
          [:div.selection-listing
           (doall
            (for [selection (filter-selections @*search-text selections)]
              ^{:key (str "selection-" selection)}
              [:div.selection
               {:on-click
                (fn []
                  (reset! *current-selection selection)
                  (reset! *search-text "")
                  (reset! *open? false)
                  (when on-select (on-select selection)))}
               selection]))]])])))
