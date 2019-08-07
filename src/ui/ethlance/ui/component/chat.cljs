(ns ethlance.ui.component.chat
  (:require
   [reagent.core :as r]

   ;; Ethlance Components
   [ethlance.ui.component.profile-image :refer [c-profile-image]]))


;; TODO: format 'text' into paragraphs <p>
(defn c-chat-message
  [{:keys [user-type text full-name date-created date-updated details image-url] :as message}]
  (let [position-class (if (contains? #{:candidate :arbiter} user-type) " right " " left ")
        color-class (case user-type
                      :candidate " candidate "
                      :employer " employer "
                      :arbiter " arbiter ")]
    (fn []
      [:div.ethlance-chat-message
       {:class [position-class color-class]}
       [:div.details
        [:div.profile-image [c-profile-image {:src "#"}]]
        [:span.full-name {:key "detail-full-name"} full-name]
        [:div.info-listing
         (doall
          (for [detail details]
            ^{:key (str "detail-" detail)}
            [:span.info detail]))]
        [:span.date-updated date-updated]]
       [:div.text text]])))


(defn c-chat-log
  [chat-listing]
  [:div.ethlance-chat-log
   (doall
    (for [message chat-listing]
     ^{:key (str "chat-message-" (:id message))}
     [c-chat-message message]))])
