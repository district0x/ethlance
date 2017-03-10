(ns ethlance.components.message-bubble
  (:require
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a]]
    [ethlance.components.profile-picture :refer [profile-picture]]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    ))

(defn date-line [date]
  [col {:xs 12}
   [row
    {:center "xs"
     :style styles/message-bubble-time}
    [col {:xs 12}
     "- " (u/format-datetime date) " -"]]])

(defn message-bubble* [{:keys [:side :header :lines]} text]
  [col
   (r/merge-props
     {:xs 10 :md 9}
     (when (= side :right)
       {:xs-offset 0 :md-offset 1}))
   [row-plain
    {:style (if (= side :right)
              styles/message-bubble-right
              styles/message-bubble-left)}
    header
    [misc/long-text
     {:style styles/full-width}
     text]]])

(defn profile-picture* []
  (let [xs-width? (subscribe [:window/xs-width?])]
    (fn [{:keys [side user]}]
      [col
       (r/merge-props
         {:xs 2}
         (when (= side :right)
           {:style styles/text-right}))
       [profile-picture {:user user
                         :employer? (= side :right)
                         :size (if @xs-width? 40 70)
                         :hide-name? @xs-width?}]])))

(defn message-bubble [{:keys [:side :date :user :key]
                       :as props
                       :or {side :left}} body]
  [row
   (merge
     {:style styles/full-width}
     (when key
       {:key key}))
   (when date
     [date-line date])
   (let [props (dissoc props :key)]
     [col {:xs 12
           :style styles/message-bubble-row}
      (if (= side :left)
        [row
         [profile-picture* props]
         [message-bubble* props body]]
        [row
         [message-bubble* props body]
         [profile-picture* props]])])])