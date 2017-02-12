(ns ethlance.components.user-detail
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.languages-chips :refer [languages-chips]]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a center-layout currency]]
    [ethlance.components.star-rating :refer [star-rating]]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [re-frame.core :refer [subscribe dispatch dispatch-sync]]
    [ethlance.components.icons :as icons]
    [reagent.core :as r]))

(defn user-address [address]
  [:div
   {:style (merge styles/margin-bottom-gutter-less
                  styles/word-wrap-break)}
   [:a {:target :_blank
        :style {:color styles/primary1-color}
        :href (u/etherscan-url address)} address]])

(defn blocked-user-chip []
  [misc/status-chip
   {:background-color styles/danger-color}
   "user is blocked"])

(defn user-created-on [created-on]
  [:h4 {:style (merge styles/fade-text
                      {:margin-bottom 5})} "joined on " (u/format-date created-on)])

(defn languages-section [{:keys [:user/id] :as user} languages]
  [:div
   [misc/subheader "Speaks languages"]
   [misc/call-on-change
    {:load-on-mount? true
     :args {id (select-keys user [:user/languages-count])}
     :on-change #(dispatch [:after-eth-contracts-loaded [:contract.db/load-user-languages %]])}
    [languages-chips
     {:value languages}]]])

(defn user-info []
  (let [xs-width? (subscribe [:window/xs-width?])]
    (fn [{:keys [:user/gravatar :user/name :user/country :user/state :user/created-on :user/languages :user/id
                 :user/status :user/address :user/balance :user/github :user/linkedin] :as user}
         {:keys [:avg-rating :ratings-count :employer/total-paid :description :show-availability? :freelancer/available?
                 :freelancer/total-earned :freelancer/job-title :freelancer/hourly-rate
                 :freelancer/hourly-rate-currency]}]
      [:div
       [row
        {:middle "xs"
         :center "xs"
         :start "sm"}
        [col
         {:xs 12 :sm 2}
         [ui/avatar
          {:size (if @xs-width? 150 100)
           :src (u/gravatar-url gravatar id)}]]
        [col
         {:xs 12 :sm 6 :lg 7
          :style (if @xs-width? {:margin-top 10} {})}
         [:h1 name]
         (when job-title
           [:h3 job-title])
         [star-rating
          {:value (u/rating->star avg-rating)
           :show-number? true
           :center "xs"
           :start "sm"
           :ratings-count ratings-count
           :style (if @xs-width? {:margin-top 5} {})}]
         [misc/country-marker
          {:row-props {:center "xs"
                       :start "sm"
                       :style (if @xs-width? {:margin-top 5} {})}
           :country country
           :state state}]]
        [col {:xs 12 :sm 4 :lg 3
              :style (if-not @xs-width? styles/text-right {})}
         [row-plain
          {:center "xs"
           :end "sm"}
          (if (= status 2)
            [blocked-user-chip]
            (when show-availability?
              [misc/status-chip
               {:style (merge {:margin-bottom 5}
                              (when @xs-width? {:margin-top 5}))
                :background-color (styles/freelancer-available?-color available?)}
               (if available?
                 "available for hire!"
                 "not available for hire")]))]
         (when hourly-rate
           [misc/elegant-line "hourly rate" [currency hourly-rate {:value-currency hourly-rate-currency}]])
         (when total-paid
           [misc/elegant-line "spent" [currency total-paid]])
         (when total-earned
           [misc/elegant-line "earned" [currency total-earned]])
         [misc/elegant-line "balance" [currency balance]]]]
       [misc/hr]
       [row
        {:between "xs"
         :top "xs"}
        [col
         {:xs 12 :sm 8}
         [user-address address]]
        [col
         {:xs 12 :sm 4
          :style (if @xs-width? (merge styles/text-left
                                       {:margin-bottom 5})
                                styles/text-right)}
         (for [[base-url profile icon color] [["https://github.com/" github icons/github "#000"]
                                              ["https://www.linkedin.com/in/" linkedin icons/linkedin "#127cb3"]]]
           (when (seq profile)
             [:span {:style {:margin-left 5}
                     :key base-url}
              [misc/link
               (str base-url profile)
               [icon {:style (if @xs-width? {:width 35 :height 35} {})
                      :color color}]]]))]]
       [user-created-on created-on]
       [misc/long-text
        description]])))
