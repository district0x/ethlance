(ns ethlance.ui.component.radio-select
  "Component for performing radio selections"
  (:require
   [clojure.core.async :as async :refer [go go-loop <! >! chan close! put! timeout] :include-macros true]
   [reagent.core :as r]

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
  [{:keys [default-selection on-selection]} & children]
  (let [*currently-active (r/atom default-selection)
        select-channel (chan 1)]
    (r/create-class
     {:display-name "ethlance-radio-select"

      :component-did-mount
      (fn [this]
        (go-loop []
          (when-let [selection (<! select-channel)]
            (reset! *currently-active selection)
            (when on-selection (on-selection selection))
            (recur))))

      :component-will-unmount
      (fn [this]
        (close! select-channel))
      
      :reagent-render
      (fn [opts & children]
        [:div.ethlance-radio-select
         (doall
          (for [[selection-key child-element] children]
            ^{:key (str selection-key)}
            [c-radio-element
             {:selection-key selection-key
              :select-channel select-channel
              :child-element child-element
              :currently-active @*currently-active}]))])})))


(defn c-radio-search-filter-element [label]
  [:<>
   [c-inline-svg
    {:src "./images/svg/radio-button.svg"
     :width 24
     :height 24}]
   [:span.label label]])
