(ns ethlance.ui.page.demo
  (:require
   [FlatList]
   [taoensso.timbre :as log]
   [ethlance.ui.graphql.client :as client]
   [ethlance.ui.graphql.queries :refer [component->query]]
   [reagent.core :as reagent]))

(def scroll-interval 10)

(def flat-list (reagent/adapt-react-class FlatList))

(defn row-renderer [data]
  (let [{:keys [:address :full-name :profile-image]} (js->clj data :keywordize-keys true)]

    (prn "@@@ ITEM" {:data data})

    [:div [:h2 full-name]]

    ))

;; TODO : infinite scroll
(defn page-element []
  (let [query [:search-users
               {:limit :$limit
                :offset :$offset}
               [:user/address
                :user/full-name
                :user/profile-image]]
        {:keys [:data :error :loading? :fetch-more]} (client/use-query {:queries [query]
                                                                        :variables [{:variable/name :$limit
                                                                                     :variable/type :Int}
                                                                                    {:variable/name :$offset
                                                                                     :variable/type :Int}]
                                                                        :operation {:operation/type :query
                                                                                    :operation/name (:ethlance.ui.page.demo/page component->query)}}
                                                                       {:variables {:limit scroll-interval
                                                                                    :offset 0}})
        items (-> data :search-users)]

    (when error
      (log/error "Error calling graphql endpoint" {:error error}))

    (reagent/as-element
     (if loading?
       [:div
        [:h2 "Loading.."]]
       [flat-list {:list items
                   :renderItem (fn [item index]
                                 (reagent/as-element
                                  ^{:key index} [row-renderer item]))


                   }]))))

(defn page []
  [:> page-element])
