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
  "Select Input Component for a dropdown listing of selections. Can also
  include a search box for easier navigation.

  # Keyword Arguments

  opts - React Props and Optional Arguments

  # Optional Arguments (opts)

  :label - Placeholder Text for the selection input

  :selections - Collection of selections that can be made within the selection input.

  :on-select - Callback function called when the selection is changed. (fn [selection]).

  :default-selection - Default selection to make on initial load. [default: nil]

  :color - Component Color Styling. `:primary`,
  `:secondary`. [default: `:primary`].

  :search-bar? - If true, will include a search bar for searching
  through selections. [default: false]

  :default-search-text - Placeholder Text for the search
  bar. [default: \"Search\"]

  :size - Component Size Styling. `:large`, `:default`. [default:
  `:default`]"
  [{:keys [label 
           selections 
           on-select 
           default-selection 
           color 
           search-bar? 
           default-search-text 
           size]
    :or {default-search-text "Search"}
    :as opts}]
  (let [*open? (r/atom false)
        *current-selection (r/atom default-selection)
        *search-text (r/atom "")
        color (or color :primary)
        color-class (case color
                      :primary " primary "
                      :secondary " secondary ")
        icon-color (case color
                     :primary :dark-blue
                     :secondary :white)
        size (or size :default)
        size-class (case size
                     :large " large "
                     :default nil)]
    (fn [{:keys [label selections on-select default-selection color] :as opts}]
      (let [opts (dissoc opts
                         :label :selections
                         :on-select :default-selection
                         :color :search-bar?
                         :default-search-text
                         :size)]
        [:div.ethlance-select-input (merge {:class [color-class size-class]} opts)
         [:div.main
          {:on-click #(swap! *open? not)}
          [:span.label (or @*current-selection label)]
          [c-icon {:class "icon"
                   :name (if @*open? :ic-arrow-up :ic-arrow-down)
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
               [c-icon {:name :close
                        :size :x-small
                        :title "Clear Search"
                        :on-click #(reset! *search-text "")}]])
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
                 selection]))]])]))))
