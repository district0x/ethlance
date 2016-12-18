(ns ethlance.pages.search-freelancers-page
  (:require
    [cljs-react-material-ui.icons :as icons]
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.category-select-field :refer [category-select-field]]
    [ethlance.components.checkbox-group :refer [checkbox-group]]
    [ethlance.components.country-auto-complete :refer [country-auto-complete]]
    [ethlance.components.language-select-field :refer [language-select-field]]
    [ethlance.components.misc :as misc :refer [col row paper-thin row-plain a]]
    [ethlance.components.skills-chip-input :refer [skills-chip-input]]
    [ethlance.components.skills-chips :refer [skills-chips]]
    [ethlance.components.slider-with-counter :refer [slider-with-counter]]
    [ethlance.components.star-rating :refer [star-rating]]
    [ethlance.components.truncated-text :refer [truncated-text]]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [goog.string :as gstring]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn filter-sidebar []
  (let [form-data (subscribe [:form/search-freelancers])]
    (dispatch [:after-eth-contracts-loaded [:contract.search/search-freelancers @form-data]])
    (fn []
      (let [{:keys [:search/category :search/skills :search/min-avg-rating
                    :search/min-freelancer-ratings-count :search/min-hourly-rate :search/max-hourly-rate
                    :search/country :search/language :search/offset :search/limit]} @form-data]
        [paper-thin
         [category-select-field
          {:value category
           :full-width true
           :on-change #(dispatch [:form/search-freelancers-changed :search/category %3])}]
         [misc/subheader "Min. Rating"]
         [star-rating
          {:value (u/rating->star min-avg-rating)
           :on-star-click #(dispatch [:form/search-freelancers-changed :search/min-avg-rating
                                      (u/star->rating %1)])}]
         [ui/text-field
          {:floating-label-text "Min. Hourly Rate"
           :type :number
           :default-value min-hourly-rate
           :full-width true
           :min 0
           :on-change #(dispatch [:form/search-freelancers-changed :search/min-hourly-rate %2])}]
         [ui/text-field
          {:floating-label-text "Max. Hourly Rate"
           :type :number
           :default-value max-hourly-rate
           :full-width true
           :min 0
           :on-change #(dispatch [:form/search-freelancers-changed :search/max-hourly-rate %2])}]
         [ui/text-field
          {:floating-label-text "Min. Number of Feedbacks"
           :type :number
           :default-value min-freelancer-ratings-count
           :full-width true
           :min 0
           :on-change #(dispatch [:form/search-freelancers-changed :search/min-freelancer-ratings-count %2])}]
         [country-auto-complete
          {:value country
           :full-width true
           :on-new-request #(dispatch [:form/search-freelancers-changed :search/country %2])}]
         [language-select-field
          {:value language
           :full-width true
           :on-new-request #(dispatch [:form/search-freelancers-changed :search/language %2])}]]))))

(defn search-results []
  (let [list (subscribe [:list/search-freelancers])]
    (fn []
      (let [{:keys [loading? items]} @list]
        [paper-thin
         {:loading? loading?}
         (if (seq items)
           (for [{:keys [:freelancer/avg-rating :freelancer/hourly-rate :freelancer/job-title
                         :freelancer/ratings-count :freelancer/skills
                         :user/id :user/name :user/gravatar :user/country] :as item} items]
             [row
              {:key id
               :middle "xs"}
              [col
               {:md 1}
               [ui/avatar
                {:size 55
                 :src (u/gravatar-url gravatar)}]]
              [col
               [:h2 [a {:style styles/primary-text
                        :route :freelancer/detail
                        :route-params {:user/id id}}
                     name]]
               [:div {:style styles/fade-text} job-title]]
              [col
               {:xs 12
                :style styles/fade-text}
               [row-plain
                {:middle "xs"}
                [star-rating
                 {:value (u/rating->star avg-rating)
                  :small? true}]
                [:span [:span {:style (merge styles/dark-text
                                             styles/freelancer-info-item)}
                        (u/eth hourly-rate)] " per hour"]
                [:span
                 {:style styles/freelancer-info-item}
                 ratings-count (u/pluralize " feedback" ratings-count)]
                [misc/country-marker
                 {:country country
                  :row-props {:style styles/freelancer-info-item}}]]
               [:div
                [skills-chips
                 {:selected-skills skills}]]
               [misc/hr-small]]])
           [row {:center "xs" :middle "xs"
                 :style {:min-height 200}}
            (when-not loading?
              [:h3 "No freelancers match your search criteria :("])])]))))

(defn skills-input []
  (let [selected-skills (subscribe [:form/search-freelancer-skills])]
    (fn []
      [paper-thin
       [skills-chip-input
        {:value @selected-skills
         :hint-text "Type skills you want a freelancer to have"
         :on-change #(dispatch [:form/search-freelancers-changed :search/skills %1])}]])))

(defn search-freelancers-page []
  [misc/search-layout
   [filter-sidebar]
   [skills-input]
   [search-results]])
