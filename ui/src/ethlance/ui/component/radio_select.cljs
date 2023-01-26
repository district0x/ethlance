(ns ethlance.ui.component.radio-select
  (:require [clojure.core.async
             :as
             async
             :refer
             [<! chan close! go-loop put!]
             :include-macros
             true]
            [ethlance.ui.component.inline-svg :refer [c-inline-svg]]
            [reagent.core :as r]
            [taoensso.timbre :as log]))

(defn c-radio-element
  [{:keys [selection-key
           select-channel
           child-element
           currently-active]}]
  [:div.ethlance-radio-element
   {:on-click #(put! select-channel selection-key)
    :class (when (= selection-key currently-active) "active")}
   child-element])

(defn c-radio-select
  "Radio Select Input for collaboration with several radio elements.

  # Keyword Arguments

  opts - Optional Arguments

  # Optional Arguments

  :default-selection - Uncontrolled component selection. The 'key' of the radio element to be selected.

  :selection - Controlled component selection. The 'key' of the radio element to be selected.

  :on-selection - event callback function supplied to handle changes
  in the radio selection. Function has one parameter, consisting of
  the key of the radio element that was chosen. (fn [selection])

  :flex? - Applies the 'flex' class to the radio select component, to
  make the component 'flexible'.

  # Examples

  [c-radio-select
   {:default-selection :job
    :on-selection (fn [selection] (println \"You have chosen: \" selection))
    :flex? true}
   [:job [c-radio-secondary-element \"Job\"]]
   [:bounty [c-radio-secondary-element \"Bounty\"]]]
  "
  [{:keys [default-selection on-selection flex?]}]
  (let [*current-selection (r/atom default-selection)
        select-channel (chan 1)]
    (r/create-class
     {:display-name "ethlance-radio-select"

      :component-did-mount
      (fn []
        ;; TODO : WTF is this, why is it using async??
        (go-loop []
          (when-let [selection (<! select-channel)]
            (reset! *current-selection selection)
            (if on-selection
              (on-selection selection)
              (log/warn "No Selection Callback set for c-radio-select component"))
            (recur))))

      :component-will-unmount
      (fn []
        (close! select-channel))

      :reagent-render
      (fn [opts & children]
        (assert (not (and (:selection opts) (:default-selection opts)))
                "Component has both controlled `selection` and uncontrolled `default-selection` attributes set.")
        (let [current-selection (if (contains? opts :default-selection) @*current-selection (:selection opts))
              opts (dissoc opts :default-selection :selection :on-selection :flex?)]
          [:div.ethlance-radio-select
           (merge opts {:class (when flex? "flex")})
           (doall
            (for [[selection-key child-element] children]
              ^{:key (str selection-key)}
              [c-radio-element
               {:selection-key selection-key
                :select-channel select-channel
                :child-element child-element
                :currently-active current-selection}]))]))})))

(defn c-radio-search-filter-element [label]
  [:<>
   [c-inline-svg
    {:src "/images/svg/radio-button.svg"
     :width 24
     :height 24}]
   [:span.label label]])


(defn c-radio-secondary-element [label]
  [:div.radio-secondary-element
   [c-inline-svg
    {:src "/images/svg/radio-button.svg"
     :width 24
     :height 24}]
   [:span.label label]])
