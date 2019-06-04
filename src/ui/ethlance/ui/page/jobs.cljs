(ns ethlance.ui.page.jobs
  "General Job Listings on ethlance"
  (:require
   [district.ui.component.page :refer [page]]

   ;; Ethlance Components
   [ethlance.ui.component.main-layout :refer [c-main-layout]]
   [ethlance.ui.component.rating :refer [c-rating]]
   [ethlance.ui.component.tag :refer [c-tag c-tag-label]]))


(defn c-job-search-filter
  "Sidebar component for changing the search criteria."
  []
  [:div.job-search-filter
   {:key "search-filter"}
   
   ;; TODO: implement
   [:div.category-selector "All Categories"]

   [:span.rating-label "Min. Rating"]
   [c-rating {:rating 1 :color :white :size :small
              :on-change (fn [index] (println "Min. Rating: " index))}]

   [:span.rating-label "Max. Rating"]
   [c-rating {:rating 5 :color :white :size :small
              :on-change (fn [index] (println "Max. Rating: " index))}]])

   ;; TODO: Input currency component

   ;; TODO: Radio selector component

   
(defn c-job-search-input
  "Main search bar at the top of the listing."
  []
  [:div.job-search-input
   {:key "search-input"}
   "job search input"])


(defn c-job-element
  "A single job element component composed from the job data."
  [job]
  [:div.job-element
   [:div.title "Ethereum Contract Implementation"]
   [:div.description "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Morbi ac ex non ipsum laoreet fringilla quis vel nibh. Praesent sed condimentum ex, consectetur gravida felis. Sed tincidunt vestibulum ante elementum pellentesque."]
   [:div.date "Posted 1 day ago | 5 Proposals"]
   [:div.tags
    [c-tag {} [c-tag-label "System Administration"]]
    [c-tag {} [c-tag-label "Game Design"]]
    [c-tag {} [c-tag-label "C++ Programming"]]
    [c-tag {} [c-tag-label "HopScotch Master"]]]

   [:div.users
    [:span "Brian Curran"]
    [:span "Brian Curran"]]
    ;;TODO: user component

   [:div.details "details"]])
    ;;TODO: table


(defn c-job-listing []
  [:<>
   (doall
    (for [job (range 10)]
      ^{:key (str "job-" job)}
      [c-job-element job]))])


(defmethod page :route.job/jobs []
  (let []
    (fn []
      [c-main-layout {:container-opts {:class :jobs-main-container}}
       [c-job-search-filter]
       [:div.job-listing {:key "listing"}
        [c-job-search-input]
        [c-job-listing]]])))
