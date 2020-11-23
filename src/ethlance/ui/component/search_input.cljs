(ns ethlance.ui.component.search-input
  (:require [cuerdas.core :as string]
            [ethlance.ui.component.icon :refer [c-icon]]
            [reagent.core :as r]
            [reagent.dom :as rdom]))

(def blur-delay-ms 200)

(defn filter-selections
  [search-text selections]
  (if (and (seq search-text) (seq selections))
    (->> selections
         (filter #(string/includes? (string/lower %) (string/lower search-text)))
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
   {:title label}
   [:span.label label]
   [:span.close-button
    {:on-click on-close
     :title (str "Remove '" label "'")}
    [c-icon {:name :close :size :x-small :color :black :inline? false}]]])

(defn c-chip-search-input
  "A standalone component for handling chip search inputs.

  # Keyword Arguments

  opts - Optional Arguments

  # Optional Arguments (opts)

  :default-chip-listing - Uncontrolled component. A collection of chip searches for initial load.

  :chip-listing - Controlled component. The collection of chip searches.

  :auto-suggestion-listing - A collection of string elements, which is
  displayed as a dropdown menu to choose from while searching for new
  chips to apply to the chip search.

  :on-chip-listing-change - An event callback function, consisting of
  one parameter, containing the currently applied
  chips. (fn [chip-listing])

  :allow-custom-chips? - If true, will allow the user to supply any
  custom chips that do not need to adhere to the supplied listing
  within `:auto-suggestiong-listing`. [:default false]

  :search-icon? - If true, will display a styled search icon within
  the component. [default: true].

  :placeholder - Input Placeholder text to display in the chip search
  component. [default: 'Search Tags']

  :display-listing-on-focus? - If true, the listing of search results
  will show upon focusing the main input."
  [{:keys [default-chip-listing]}]
  (let [*current-chip-listing (r/atom (or default-chip-listing #{}))
        *active-suggestion (r/atom nil)
        *search-text (r/atom "")
        *input-focused? (r/atom false)]
    (r/create-class
      {:display-name "ethlance-chip-search-input"
       :component-did-mount
       (fn [this]
         (let [root-dom (rdom/dom-node this)
               search-input (.querySelector root-dom ".search-input")]
           (.addEventListener
             search-input "blur"
             (fn []
               ;; Needs to be on a timeout for dropdown selections to work correctly.
               (.setTimeout
                 js/window
                 (fn []
                   (reset! *active-suggestion nil)
                   (reset! *search-text "")
                   (reset! *input-focused? false))
                 blur-delay-ms))
             true)

           ;; Keep track of when the input is focused
           (.addEventListener
             search-input "focus"
             (fn []
               (.setTimeout
                 js/window
                 (fn []
                   (reset! *input-focused? true))
                 0))
             true)))

       :reagent-render
       (fn [{:keys [chip-listing
                    auto-suggestion-listing
                    on-chip-listing-change
                    allow-custom-chips?
                    search-icon?
                    placeholder
                    display-listing-on-focus?]
             :or {search-icon? true
                  placeholder "Search Tags"}
             :as opts}]
         (let [;; Local Function for handling updates
               -update-chip-listing
               (fn [new-chip-listing]
                 (reset! *current-chip-listing new-chip-listing)
                 (when on-chip-listing-change
                   (on-chip-listing-change new-chip-listing)))

               current-chip-listing (if (contains? opts :default-chip-listing) @*current-chip-listing chip-listing)]
           [:div.ethlance-chip-search-input
            {:key "chip-search-input"
             :class (when-not search-icon? "no-search-icon")}
            [:div.search-container
             (doall
               (for [chip-label current-chip-listing]
                 ^{:key (str "chip-" chip-label)}
                 [c-chip
                  {:on-close #(-update-chip-listing (disj current-chip-listing chip-label))}
                  chip-label]))
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
                         (-update-chip-listing (conj current-chip-listing @*active-suggestion))
                         (reset! *search-text "")
                         (reset! *active-suggestion nil))
                       (and (> (count @*search-text) 0) allow-custom-chips?)
                       (do
                         (-update-chip-listing (conj current-chip-listing @*search-text))
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
               :placeholder (when (empty? current-chip-listing) placeholder)}]]

            (when search-icon?
              [:div.search-button [c-icon {:name :search :size :normal :inline? false}]])

            (let [auto-suggestion-listing (apply (partial disj (set auto-suggestion-listing)) current-chip-listing)
                  suggestions (or (filter-selections @*search-text auto-suggestion-listing) auto-suggestion-listing)]
              (when (or (seq @*search-text) (and display-listing-on-focus? @*input-focused?))
                [:div.dropdown
                 [:div.suggestion-listing
                  (doall
                    (for [suggestion suggestions]
                      ^{:key (str "suggestion-" suggestion)}
                      [:div.suggestion
                       {:class (when (= @*active-suggestion suggestion) "active")
                        :on-click (fn []
                                    (-update-chip-listing (set (concat current-chip-listing [suggestion])))
                                    (reset! *search-text "")
                                    (reset! *active-suggestion nil))}
                       suggestion]))]]))]))})))
