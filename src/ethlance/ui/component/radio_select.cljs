(ns ethlance.ui.component.radio-select
  "Component for performing radio selections"
  (:require
   [clojure.core.async :as async :refer [go go-loop <! >! chan close! put! timeout] :include-macros true]
   [reagent.core :as r]
   [taoensso.timbre :as log]

   [ethlance.ui.component.inline-svg :refer [c-inline-svg]]))


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

  :default-selection - The default 'key' of the radio element to be selected on initial load.

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
  [{:keys [default-selection on-selection flex?] :as opts} & children]
  (let [*currently-active (r/atom default-selection)
        select-channel (chan 1)]
    (r/create-class
     {:display-name "ethlance-radio-select"

      :component-did-mount
      (fn [this]
        (go-loop []
          (when-let [selection (<! select-channel)]
            (reset! *currently-active selection)
            (if on-selection
              (on-selection selection)
              (log/warn "No Selection Callback set for c-radio-select component"))
            (recur))))

      :component-will-unmount
      (fn [this]
        (close! select-channel))
      
      :reagent-render
      (fn [opts & children]
        (let [opts (dissoc opts :default-selection :on-selection :flex?)]
          [:div.ethlance-radio-select
           (merge opts {:class (when flex? "flex")})
           (doall
            (for [[selection-key child-element] children]
              ^{:key (str selection-key)}
              [c-radio-element
               {:selection-key selection-key
                :select-channel select-channel
                :child-element child-element
                :currently-active @*currently-active}]))]))})))


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
