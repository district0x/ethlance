(ns ethlance.ui.component.file-drag-input
  (:require
    [clojure.string :as str]
    [ethlance.ui.component.icon :refer [c-icon]]))


(def empty-img-src "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=")


(defn id-for-path
  [path]
  (if (sequential? path)
    (str/join "-" (map name path))
    (name path)))


(defn c-file-drag-input
  [{:keys [form-data id file-accept-pred on-file-accepted on-file-rejected]
    :or {file-accept-pred (constantly true)}}]
  (let [allow-drop #(.preventDefault %)
        handle-files-select (fn [files]
                              (when-let [f (aget files 0)]
                                (let [fprops {:name (.-name f)
                                              :type (.-type f)
                                              :size (.-size f)
                                              :file f}]
                                  (if (file-accept-pred fprops)
                                    (let [url-reader (js/FileReader.)
                                          ab-reader (js/FileReader.)]
                                      (set! (.-onload url-reader) (fn [e]
                                                                    (let [img-data (-> e .-target .-result)
                                                                          fmap (assoc fprops :url-data img-data)]
                                                                      (swap! form-data assoc-in [id :selected-file] fmap))))
                                      (.readAsDataURL url-reader f)
                                      (set! (.-onload ab-reader) (fn [e]
                                                                   (let [img-data (-> e .-target .-result)
                                                                         fmap (assoc fprops :array-buffer img-data)]
                                                                     (swap! form-data update id merge fmap)
                                                                     (when on-file-accepted (on-file-accepted fmap)))))
                                      (.readAsArrayBuffer ab-reader f))
                                    (when on-file-rejected
                                      (on-file-rejected fprops))))))]
    (fn [{:keys [form-data id current-src]
          :as opts}]
      (let [{:keys [url-data]} (get-in @form-data [id :selected-file])
            file-input-id (id-for-path id)]
        [:label.dropzone
         {:htmlFor file-input-id
          :on-drag-over allow-drop
          :on-drop #(do
                      (.preventDefault %)
                      (handle-files-select (.. % -dataTransfer -files)))
          :on-drag-enter allow-drop}

         [:img {:src (or url-data current-src empty-img-src)}]

         (when (not (or url-data current-src))
           [:div.file-input-label
            [c-icon {:name :ic-upload :color :dark-blue :inline? false}]
            [:div (get opts :label "File...")]])

         [:input {:type :file
                  :id file-input-id
                  :on-change (fn [e]
                               (handle-files-select (-> e .-target .-files)))}]]))))
