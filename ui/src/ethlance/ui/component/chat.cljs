(ns ethlance.ui.component.chat
  (:require [ethlance.ui.component.profile-image :refer [c-profile-image]]))

;; TODO: format 'text' into paragraphs <p>
(defn c-chat-message
  "Individual Chat Message Components to be used within `c-chat-log` component.

  # Keyword Arguments

  message - ethlance chat message object
  "
  [{:keys [id user-type text name date-updated details image-url]}]
  (let [position-class (if (contains? #{:candidate :arbiter} user-type) " right " " left ")
        color-class (case user-type
                      :candidate "candidate"
                      :employer "employer"
                      :arbiter "arbiter")]
    (fn []
      [:div.ethlance-chat-message
       {:key (str "chat-message-" id)
        :class [position-class color-class]}
       [:div.details
        [:div.profile-image [c-profile-image {:src image-url}]]
        [:div.info-container
         [:span.full-name {:key "detail-full-name"} name]
         [:div.info-listing
          (doall
           (for [detail details]
             ^{:key (str "detail-" detail)}
             [:span.info detail]))]
         [:span.date-updated date-updated]]]
       [:div.text text]])))


(defn c-chat-log
  "Chat log component, containing the `c-chat-message` components

  # Keyword Arguments

  chat-listing - Collection of ethlance chat message objects

  # Examples

  ```clojure
  [c-chat-log
   [message-object-1
    message-object-2]]
  ```
  "
  [chat-listing]
  [:div.ethlance-chat-log
   (doall
    (for [message chat-listing]
     ^{:key (str "chat-message-" (:id message))}
     [c-chat-message message]))])
