(ns ethlance.components.message-bubble
  (:require
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a]]
    [ethlance.components.profile-picture :refer [profile-picture]]
    [ethlance.components.truncated-text :refer [truncated-text]]
    [ethlance.styles :as styles]
    [reagent.core :as r]
    [ethlance.utils :as u]))

(defn date-line [date]
  [col {:xs 12}
   [row
    {:center "xs"
     :style styles/message-bubble-time}
    [col {:xs 12}
     "- " (u/format-date date) " -"]]])

(defn message-bubble* [{:keys [side truncate?]} body]
  [col
   (r/merge-props
     {:xs 10 :md 9}
     (when (= side :right)
       {:xs-offset 0 :md-offset 1}))
   [row-plain
    {:style (if (= side :right)
              styles/message-bubble-right
              styles/message-bubble-left)}
    (if truncate?
      [truncated-text body]
      body)]])

(defn profile-picture* [{:keys [side user]}]
  [col
   (r/merge-props
     {:xs 2}
     (when (= side :right)
       {:style styles/text-right}))
   [profile-picture {:user user
                     :employer? (= side :right)}]])

(defn message-bubble [{:keys [side date user]
                       :as props
                       :or {side :left}} body]
  [row
   {:style styles/full-width}
   (when date
     [date-line date])
   [col {:xs 12
         :style styles/message-bubble-row}
    (if (= side :left)
      [row
       [profile-picture* props]
       [message-bubble* props body]]
      [row
       [message-bubble* props body]
       [profile-picture* props]])]])