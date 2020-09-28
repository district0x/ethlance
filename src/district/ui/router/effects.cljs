(ns district.ui.router.effects
  (:require
    [bide.core :as bide]
    [cljs.spec.alpha :as s]
    [re-frame.core :as re :refer [reg-fx dispatch]]))

(reg-fx
  ::navigate
  (fn [[& args]]
    (apply bide/navigate! args)))


(reg-fx
  ::replace
  (fn [[& args]]
    (apply bide/replace! args)))


(defn- post-event-callback-fn [opts]
  (fn [event-v]
    (let [watched-name (:name opts)
          watched-params (:params opts)
          watched-query (:query opts)
          [event-name name params query] event-v]
      (when (and (= :district.ui.router.events/active-page-changed event-name)
                 (or (nil? watched-name)
                     (and (or (keyword? watched-name)
                              (string? watched-name))
                          (= watched-name name))
                     (and (sequential? watched-name)
                          (contains? (set watched-name) name))
                     (and (fn? watched-name)
                          (watched-name name)))
                 (or (nil? watched-params)
                     (and (map? watched-params)
                          (= watched-params params))
                     (and (fn? watched-params)
                          (watched-params params)))
                 (or (nil? watched-query)
                     (and (map? watched-query)
                          (= watched-query query))
                     (and (fn? watched-query)
                          (watched-query query))))
        (dispatch (conj (vec (:dispatch opts)) name params query))
        (when (seq (:dispatch-n opts))
          (doseq [dispatch-v (remove nil? (:dispatch-n opts))]
            (dispatch (conj (vec dispatch-v) name params query))))))))



(s/def ::id (complement nil?))
(s/def ::watcher (s/keys :req-un [::id]))
(s/def ::watchers (s/coll-of ::watcher))

(reg-fx
  ::watch-active-page
  (fn [watchers]
    (let [watchers (if (sequential? watchers) watchers [watchers])]
      (s/assert ::watchers watchers)
      (doseq [{:keys [:id] :as watch-opts} watchers]
        (re/add-post-event-callback id (post-event-callback-fn watch-opts))))))


(reg-fx
  ::unwatch-active-page
  (fn [watchers]
    (let [watchers (if (sequential? watchers) watchers [watchers])]
      (s/assert ::watchers watchers)
      (doseq [{:keys [:id]} watchers]
        (re/remove-post-event-callback id)))))

