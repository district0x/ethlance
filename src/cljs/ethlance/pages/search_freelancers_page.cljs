(ns ethlance.pages.search-freelancers-page
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.category-select-field :refer [category-select-field]]
    [ethlance.components.checkbox-group :refer [checkbox-group]]
    [ethlance.components.country-auto-complete :refer [country-auto-complete]]
    [ethlance.components.language-select-field :refer [language-select-field]]
    [ethlance.components.misc :as misc :refer [col row paper-thin row-plain a currency]]
    [ethlance.components.search :refer [skills-input]]
    [ethlance.components.search-results :as search-results]
    [ethlance.components.skills-chip-input :refer [skills-chip-input]]
    [ethlance.components.skills-chips :refer [skills-chips]]
    [ethlance.components.slider-with-counter :refer [slider-with-counter]]
    [ethlance.components.star-rating :refer [star-rating]]
    [ethlance.components.state-select-field :refer [state-select-field]]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [goog.string :as gstring]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn filter-sidebar []
  (let [form-data (subscribe [:form/search-freelancers])
        xs-sm-width? (subscribe [:window/xs-sm-width?])]
    (fn []
      (let [{:keys [:search/category :search/skills :search/min-avg-rating
                    :search/min-freelancer-ratings-count :search/min-hourly-rate :search/max-hourly-rate
                    :search/country :search/state :search/language :search/offset :search/limit
                    :search/hourly-rate-currency]} @form-data]
        [misc/call-on-change
         {:load-on-mount? true
          :args @form-data
          :on-change #(dispatch [:after-eth-contracts-loaded [:contract.search/search-freelancers-deb @form-data]])}
         [search-results/search-paper-thin
          [category-select-field
           {:value category
            :full-width true
            :on-change #(dispatch [:form.search/set-value :search/category %3])}]
          [misc/subheader "Min. Rating"]
          [star-rating
           {:value (u/rating->star min-avg-rating)
            :on-star-click #(dispatch [:form.search/set-value :search/min-avg-rating
                                       (u/star->rating %1)])}]
          [misc/ether-field-with-currency-select-field
           {:ether-field-props
            {:floating-label-text "Min. Hourly Rate"
             :value min-hourly-rate
             :full-width true
             :allow-empty? true
             :on-change #(dispatch [:form.search/set-value :search/min-hourly-rate %2 u/non-neg-or-empty-ether-value?])}
            :currency-select-field-props
            {:value hourly-rate-currency
             :on-change (fn [_ _ currency]
                          (dispatch [:form.search/set-value :search/hourly-rate-currency currency])
                          (dispatch [:selected-currency/set currency]))}}]
          [misc/ether-field-with-currency-select-field
           {:ether-field-props
            {:floating-label-text "Max. Hourly Rate"
             :value max-hourly-rate
             :full-width true
             :allow-empty? true
             :on-change #(dispatch [:form.search/set-value :search/max-hourly-rate %2 u/non-neg-or-empty-ether-value?])}
            :currency-select-field-props
            {:value hourly-rate-currency
             :on-change (fn [_ _ currency]
                          (dispatch [:form.search/set-value :search/hourly-rate-currency currency])
                          (dispatch [:selected-currency/set currency]))}}]
          [misc/text-field-base
           {:floating-label-text "Min. Number of Feedbacks"
            :type :number
            :value min-freelancer-ratings-count
            :full-width true
            :min 0
            :on-change #(dispatch [:form.search/set-value :search/min-freelancer-ratings-count %2])}]
          [country-auto-complete
           (merge
             {:value country
              :full-width true
              :on-new-request #(dispatch [:form.search/set-value :search/country %2])}
             (when @xs-sm-width?
               {:target-origin {:vertical "bottom" :horizontal "left"}
                :max-search-results 3}))]
          (when (u/united-states? country)
            [state-select-field
             {:value state
              :full-width true
              :on-change #(dispatch [:form.search/set-value :search/state %3])}])
          [language-select-field
           (merge
             {:value language
              :full-width true
              :on-new-request #(dispatch [:form.search/set-value :search/language %2])}
             (when @xs-sm-width?
               {:target-origin {:vertical "bottom" :horizontal "left"}
                :max-search-results 3}))]
          [misc/search-filter-reset-button]
          [misc/search-filter-done-button
           {:on-touch-tap #(dispatch [:search-filter.freelancers/set-open? false])}]]]))))

(defn change-page [new-offset]
  (dispatch [:form.search/set-value :search/offset new-offset])
  (dispatch [:window/scroll-to-top]))

(defn search-results []
  (let [list (subscribe [:list/search-freelancers])
        selected-skills (subscribe [:form/search-freelancer-skills])
        form-data (subscribe [:form/search-freelancers])
        xs-width? (subscribe [:window/xs-width?])]
    (fn []
      (let [{:keys [loading? items]} @list
            {:keys [:search/offset :search/limit]} @form-data
            xs? @xs-width?
            selected-skills @selected-skills]
        [search-results/search-results
         {:items-count (count items)
          :loading? loading?
          :offset offset
          :limit limit
          :no-items-found-text "No freelancers match your search criteria"
          :no-more-items-text "No more freelancers found"
          :next-button-text "Next"
          :prev-button-text "Previous"
          :on-page-change change-page}
         (for [{:keys [:freelancer/avg-rating :freelancer/hourly-rate :freelancer/hourly-rate-currency
                       :freelancer/job-title :freelancer/ratings-count :freelancer/skills
                       :user/id :user/name :user/gravatar :user/country :user/state] :as item} items]
           [row-plain
            {:key id :middle "xs" :center "xs" :start "sm"}
            [a
             {:route :freelancer/detail
              :route-params {:user/id id}}
             [ui/avatar
              {:size (if xs? 80 55)
               :src (u/gravatar-url gravatar id)
               :style (if xs?
                        {:margin-bottom 10}
                        {:margin-right 10})}]]
            [:div
             {:style (if xs? styles/full-width {})}
             [:h2 [a {:style styles/search-result-headline
                      :route :freelancer/detail
                      :route-params {:user/id id}}
                   name]]
             [:div {:style (merge styles/fade-text
                                  (when xs? {:margin-top 5}))} job-title]]
            [row-plain
             {:middle "xs" :center "xs" :start "sm"
              :style styles/freelancer-search-result-info-row}
             [star-rating
              {:value (u/rating->star avg-rating)
               :small? true}]
             [:span
              {:style styles/freelancer-info-item}
              [:span {:style styles/dark-text} ratings-count]
              (u/pluralize " feedback" ratings-count)]
             [:span [:span {:style (merge styles/dark-text
                                          styles/freelancer-info-item)}
                     [currency hourly-rate {:value-currency hourly-rate-currency}]] " per hour"]
             [misc/country-marker
              {:country country
               :state state
               :row-props {:style styles/freelancer-info-item}}]]
            [skills-chips
             {:selected-skills skills
              :max-count 7
              :on-touch-tap (fn [skill-id]
                              (when-not (contains? (set selected-skills) skill-id)
                                (dispatch [:form.search/set-value :search/skills
                                           (conj (into [] selected-skills) skill-id)])))}]
            [misc/hr-small]])]))))

(defn search-freelancers-page []
  [misc/search-layout
   {:filter-drawer-props {:open @(subscribe [:db/search-freelancers-filter-open?])
                          :on-request-change #(dispatch [:search-filter.freelancers/set-open? %])}
    :filter-open-button-props {:on-touch-tap #(dispatch [:search-filter.freelancers/set-open? true])}}
   [filter-sidebar]
   [skills-input
    {:selected-skills-subscribe [:form/search-freelancer-skills]
     :selected-skills-or-subscribe [:form/search-freelancer-skills-or]
     :open-subscribe [:db/search-freelancers-skills-open?]
     :skills-hint-text "Type skills you want a freelancer to have"
     :skills-and-hint-text "Freelancers have all of entered skills"
     :skills-or-hint-text "Freelancers have at least one of entered skills"
     :skills-and-floating-label-text "All of skills are required"
     :skills-or-floating-label-text "Any of skills is required"
     :on-toggle-open #(dispatch [:toggle-search-skills-input :search-freelancers-skills-open? :form/search-freelancers %])}]
   [search-results]])
