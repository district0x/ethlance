(ns ethlance.ui.page.demo
  (:require
   [taoensso.timbre :as log]
   [ethlance.ui.graphql.client :as client]
   [ethlance.ui.graphql.queries :refer [component->query]]
   [reagent.core :as reagent]))

(def scroll-interval 10)

(defn page-element []
  (let [query [:search-users
               {:limit :$limit
                :offset :$offset
                }
               [:user/address
                :user/full-name
                :user/profile-image
                ]]
        {:keys [:data :error :loading? :fetch-more]} (client/use-query {:queries [query]
                                                                        :variables [{:variable/name :$limit
                                                                                     :variable/type :Int}
                                                                                    {:variable/name :$offset
                                                                                     :variable/type :Int}]
                                                                        :operation {:operation/type :query
                                                                                    :operation/name (:ethlance.ui.page.demo/page component->query)}}
                                                                       {:variables {:limit scroll-interval
                                                                                    :offset 0}})]

    (when error
      (log/error "Error calling graphql endpoint" {:error error}))

    (reagent/as-element
     (if loading?
       [:div
        [:h2 "Loading.."]]

       [:div

        [:h2 (str data)]

        ]

       ))))

(defn page []
  [:> page-element])
