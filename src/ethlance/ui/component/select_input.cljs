(ns ethlance.ui.component.select-input
  (:require [cuerdas.core :as string]
            [ethlance.ui.component.icon :refer [c-icon]]
            [reagent.core :as r]))

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

  :default-selection - Uncontrolled component selection.

  :selection - Controlled component selection.

  :color - Component Color Styling. `:primary`,
  `:secondary`. [default: `:primary`].

  :search-bar? - If true, will include a search bar for searching
  through selections. [default: false]

  :default-search-text - Placeholder Text for the search
  bar. [default: \"Search\"]

  :size - Component Size Styling. `:large`, `:default`. [default:
  `:default`]"
  [{:keys [default-selection
           color
           search-bar?
           default-search-text
           size]
    :or {default-search-text "Search"}}]
  (let [*open? (r/atom false)
        *current-default-selection (r/atom default-selection)
        *search-text (r/atom "")
        color (or color :primary)
        color-class (case color
                      :primary "primary"
                      :secondary "secondary")
        icon-color color
        size (or size :default)
        size-class (case size
                     :large "large"
                     :default nil)]
    (fn [{:keys [label selections on-select default-selection selection color] :as opts}]
      (assert (not (and selection default-selection))
              "Component has both controlled `selection` and uncontrolled `default-selection` attributes set.")
      (let [opts (dissoc opts
                         :label :selections
                         :on-select :default-selection
                         :selection
                         :color :search-bar?
                         :default-search-text
                         :size)
            current-selection (if (contains? opts :default-selection) @*current-default-selection selection)]
        [:div.ethlance-select-input (merge {:class [color-class size-class]} opts)
         [:div.main
          {:title (or current-selection label)
           :on-click #(swap! *open? not)}
          [:span.label (or current-selection label)]
          [c-icon {:class "icon"
                   :name (if @*open? :ic-arrow-up :ic-arrow-down)
                   :color icon-color
                   :inline? false
                   :size (case size
                           :large :normal
                           :default :smaller)}]]
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
                        :on-click #(reset! *search-text "")
                        :color (case color
                                 :primary :black
                                 :secondary :white
                                 :black)
                        :inline? false}]])
            [:div.selection-listing
             (doall
              (for [selection (filter-selections @*search-text selections)]
                ^{:key (str "selection-" selection)}
                [:div.selection
                 {:on-click
                  (fn []
                    (reset! *current-default-selection selection)
                    (reset! *search-text "")
                    (reset! *open? false)
                    (when on-select (on-select selection)))}
                 selection]))]])]))))
