(ns ethlance.ui.component.chat
  (:require
    [district.format :as format]
    [ethlance.ui.component.profile-image :refer [c-profile-image]]
    [ethlance.ui.util.navigation :as util.navigation]))


;; TODO: format 'text' into paragraphs <p>
(defn c-chat-message
  "Individual Chat Message Components to be used within `c-chat-log` component.

  # Keyword Arguments

  message - ethlance chat message object
  "
  [{:keys [id user-type direction text full-name sender-user-id timestamp details image-url]}]
  (let [position-class (if (= :sent direction) "left" "right")
        user-type (or user-type :candidate)
        color-class (case user-type
                      :candidate "candidate"
                      :employer "employer"
                      :arbiter "arbiter")]
    (fn []
      (let [wrap-in-link (fn [user-id child-el]
                           (if user-id
                             [:a (util.navigation/link-params
                                   {:route :route.user/profile
                                    :params {:address user-id}
                                    :query {:tab user-type}})
                              child-el]
                             child-el))]
        [:div.ethlance-chat-message
         {:key (str "chat-message-" id)
          :class [position-class color-class]}
         [:div.details
          [:div.profile-image
           (wrap-in-link sender-user-id [c-profile-image {:src image-url}])]

          [:div.info-container
           [:span.full-name {:key (str "detail-full-name-" id)} (wrap-in-link sender-user-id full-name)]

           [:div.info-listing
            (doall
              (for [detail details]
                ^{:key (str "detail-" detail)}
                [:span.info detail]))]
           ;; TODO: remove new js/Date after switching to district.ui.graphql that converts Date GQL type automatically
           [:span.date-updated (format/time-ago (new js/Date timestamp))]]]
         [:div.text text]]))))


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
       ^{:key (str "chat-message-" (:id message) "-" (hash (:text message)))}
       [c-chat-message message]))])
