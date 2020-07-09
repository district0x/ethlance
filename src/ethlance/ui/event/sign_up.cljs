(ns ethlance.ui.event.sign-up
  (:require [re-frame.core :as re]
            [taoensso.timbre :as log]
            [district.ui.logging.events :as logging]
            [district0x.re-frame.ipfs-fx]))

(re/reg-event-fx
 ::upload-user-image
 (fn [_ [_ {:keys [file-info] :as data} deposit]]
   (log/info "Uploading user image" {:file file-info} ::upload-meme)
   {:ipfs/call {:func "add"
                :args [(:file file-info)]
                :opts {:wrap-with-directory true}
                :on-success [::upload-user-image-success data deposit]
                :on-error [::logging/error "upload-meme ipfs call error" {:data data
                                                                          :deposit deposit}
                           ::upload-user-image]}}))

(re/reg-event-fx
 ::upload-user-image-success
 (fn [_ [_ & args]]
   (log/info "User image uploaded" {:args args})))
