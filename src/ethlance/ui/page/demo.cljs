(ns ethlance.ui.page.demo
  (:require
   [FlatList]
   [taoensso.timbre :as log]
   [ethlance.shared.graphql.utils :as graphql-utils]
   [ethlance.ui.graphql.client :as client]
   [ethlance.ui.graphql.queries :refer [component->query]]
   [reagent.core :as reagent]))

(def scroll-interval 10)
(def flat-list (reagent/adapt-react-class FlatList))
(def troll-face "https://www.pinpng.com/pngs/m/397-3975612_troll-face-transparent-png-troll-face-profile-png.png")

(defn button [{:keys [:label :on-click :color]}]
  [:button {:style {:background-color color
                    :border-radius 5
                    :border-width 1
                    :border-color :black
                    :padding 10
                    :color :white}
            :type "button"
            :on-click on-click}
   label])

(defn indicator []
  [:div {:style {
                 :display :flex
                 :height "100%"
                 :width "100%"
                 :align-items :center
                 :justify-content :center}}
   [:h2 {:style {:color :white}}
    "Loading..."]])

(defn row-renderer [{:keys [:index :data :update-profile :query]}]
  (let [{:keys [:address :full-name :profile-image]} (js->clj data :keywordize-keys true)]
    [:div {:style {:display :flex
                   :flex-direction :row
                   :align-items :center
                   :justify-content :space-between
                   :heigh 100
                   :background-color "#1E212C"
                   :margin-left 15
                   :margin-right 15
                   :padding 5
                   :margin-bottom 10}}
     [:h2 {:style {:width 10
                   :font-family "Avenir Heavy"
                   :font-size 17
                   :color :white
                   :opacity 1.0}}
      address]
     [:img {:src profile-image
            :style {:width 45 :height 45
                    :border-radius 45
                    :border-color :white
                    :border-width 2
                    :shadow-color "#0000004D"
                    :shadow-offset {:width 0 :height 3}
                    :shadow-radius 1}}]
     [:h2 {:style {:font-family "Avenir Heavy"
                   :font-size 17
                   :width 100
                   :color :white
                   :opacity 0.9}}
      full-name]
     [button {:label "optimistic" :color "#5A667E" :on-click (fn []
                                                               (update-profile (clj->js {:variables {:address address
                                                                                                     :photo troll-face}
                                                                                         :update (fn [cache response]
                                                                                           (let [{:keys [:user/address :user/profile-image] :as resp}
                                                                                                 (get-in (graphql-utils/gql->clj response) [:data :update-user-profile])
                                                                                                 cached-data (client/read-cache cache {:queries [query]
                                                                                                                                       ;; :variables [{:variable/name :$limit
                                                                                                                                       ;;              :variable/type :Int}
                                                                                                                                       ;;             {:variable/name :$offset
                                                                                                                                       ;;              :variable/type :Int}]
                                                                                                                                       ;; :operation {:operation/type :query
                                                                                                                                       ;;             :operation/name (:ethlance.ui.page.demo/page component->query)}

                                                                                                                                       }
                                                                                                                                ;; {:variables {:limit scroll-interval
                                                                                                                                ;;              :offset 0}}
                                                                                                                                )
                                                                                                 ;; updated-data
                                                                                                 ;; (-> cached-data
                                                                                                 ;;     (assoc-in [:user :user/is-current-user-following] is-followed)
                                                                                                 ;;     (assoc-in [:user :user/followers-count] followers-count))
                                                                                                 ]

                                                                                             (log/debug "cached data" {:r cached-data})

                                                                                             #_(apollo/write-cache cache {:queries [user-query]} updated-data))

                                                                                                   )})))}]
     [button {:label "query refetch" :color "#1414FF9B" :on-click (fn []
                                                                    (update-profile (clj->js {:variables {:address address
                                                                                                          :photo troll-face}
                                                                                              :refetchQueries [(name (:ethlance.ui.page.demo/page component->query))]})))}]]))

;; TODO : mutations
;; - cache update
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
        items (-> data :search-users)
        {update-profile :call-mutation} (client/use-mutation {:queries [[:update-user-profile {:input {:user/address :$address
                                                                                                       :user/profile-image :$photo}}
                                                                         [:user/address
                                                                          :user/profile-image]]]
                                                              :variables [{:variable/name :$address
                                                                           :variable/type :ID!}
                                                                          {:variable/name :$photo
                                                                           :variable/type :String}]
                                                              :operation {:operation/type :mutation
                                                                          :operation/name :UpdateProfile}})]

    (when error
      (log/error "Error calling graphql endpoint" {:error error}))

    (reagent/as-element
     (if loading?
       [indicator]
       [flat-list {:list items
                   :renderItem (fn [item index]
                                 (reagent/as-element
                                  ^{:key index} [row-renderer {:index index :data item :update-profile update-profile :query query}]))
                   :paginationLoadingIndicator (reagent/as-element [indicator])
                   :hasMoreItems true
                   :loadMoreItems (fn []
                                    (log/debug "loading more...")
                                    (let [from (count items)]
                                      (fetch-more (clj->js {:variables {:limit (+ scroll-interval from)
                                                                        :offset from}
                                                            :updateQuery (fn [prev new]
                                                                           (let [previous (js->clj prev :keywordize-keys true)
                                                                                 {new :fetchMoreResult} (js->clj new :keywordize-keys true)
                                                                                 previous-items (-> previous :searchUsers)
                                                                                 new-items (-> new :searchUsers)]
                                                                             (clj->js (-> (merge previous new)
                                                                                          (assoc-in [:searchUsers]
                                                                                                    (into previous-items new-items))))))}))))}]))))

(defn page []
  [:> page-element])
