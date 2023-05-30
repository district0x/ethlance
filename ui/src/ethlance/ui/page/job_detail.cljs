(ns ethlance.ui.page.job-detail
  (:require [district.ui.component.page :refer [page]]
            [district.format :as format]
            [ethlance.ui.component.button :refer [c-button c-button-label]]
            [ethlance.ui.component.carousel
             :refer
             [c-carousel c-carousel-old c-feedback-slide]]
            [ethlance.ui.component.scrollable :refer [c-scrollable]]
            [ethlance.ui.component.circle-button :refer [c-circle-icon-button]]
            [ethlance.ui.component.main-layout :refer [c-main-layout]]
            [ethlance.ui.component.profile-image :refer [c-profile-image]]
            [ethlance.ui.component.rating :refer [c-rating]]
            [ethlance.ui.component.select-input :refer [c-select-input]]
            [ethlance.ui.component.table :refer [c-table]]
            [ethlance.ui.component.tag :refer [c-tag c-tag-label]]
            [ethlance.ui.component.text-input :refer [c-text-input]]
            [ethlance.ui.component.textarea-input :refer [c-textarea-input]]
            [district.ui.graphql.subs :as gql]
            [ethlance.ui.util.component :refer [<sub >evt]]
            [ethlance.ui.util.navigation :as util.navigation]
            [ethlance.ui.util.tokens :as token-utils]
            [ethlance.shared.utils :refer [millis->relative-time ilike!=]]
            [ethlance.shared.utils :as shared-utils]
            [re-frame.core :as re]))

(defn c-token-values [{:keys [token-type token-amount token-address token-id disabled? token-symbol token-name] :as opts}]
  (let [token-type (keyword token-type)
        step (if (= token-type :eth) 0.001 1)]
    (cond
      (= :erc721 token-type)
      [:div "The payment will be NFT (ERC721)"]

     (#{:eth :erc1155 :erc20} token-type)
      [:div.amount-input
       [c-text-input
        {:placeholder "Token amount"
         :step step
         :type :number
         :default-value nil
         :disabled disabled?
         :value token-amount
         :on-change #(re/dispatch [:page.job-detail/set-proposal-token-amount (js/parseFloat %)])}]
       [:a {:href (token-utils/address->token-info-url token-address) :target "_blank"}
       [:label token-symbol]
       [:label (str "(" (or token-name (name token-type)) ")")]]])))

(defn c-invoice-listing [contract-address]
  (let [invoices-query [:job {:job/id contract-address}
                        [[:token-details [:token-detail/id
                                          :token-detail/name
                                          :token-detail/symbol]]
                         [:invoices [:total-count
                                     [:items [:id
                                              :job/id
                                              :job-story/id
                                              :invoice/status
                                              :invoice/amount-requested
                                              :invoice/amount-paid
                                              [:creation-message [:message/id
                                                                  :message/date-created
                                                                  [:creator [:user/id
                                                                             :user/name
                                                                             :user/profile-image]]]]]]]]]]
        result @(re/subscribe [::gql/query {:queries [invoices-query]}])
        job-token-symbol (get-in result [:job :token-details :token-detail/symbol])
        token->human-amount (fn [amount token-symbol]
                              (if (= token-symbol "ETH") (shared-utils/wei->eth amount) amount))
        invoices (map (fn [invoice]
                        {:name (get-in invoice [:creation-message :creator :user/name])
                         :amount (str (token->human-amount (get-in invoice [:invoice/amount-requested]) job-token-symbol) " " job-token-symbol)
                         :timestamp (format/time-ago (new js/Date (get-in invoice [:creation-message :message/date-created])))
                         :status (get-in invoice [:invoice/status])})
                      (-> result :job :invoices :items))]
    [:div.invoice-listing
          [:div.label "Invoices"]
          (into [c-table {:headers ["Candidate" "Amount" "Created" "Status"]}]
                (map (fn [invoice]
                       [[:span (:name invoice)]
                        [:span (:amount invoice)]
                        [:span (:timestamp invoice)]
                        [:span (:status invoice)]])
                     invoices))]))

(defn c-employer-feedback [contract-address]
  (let [feedback-query [:job {:job/id contract-address}
                        [[:job/employer
                          [[:employer/feedback
                            [:total-count
                             [:items [:feedback/rating
                                      :feedback/text
                                      :feedback/date-created
                                      [:feedback/from-user [:user/name
                                                            :user/profile-image]]]]]]]]]]
        result @(re/subscribe [::gql/query {:queries [feedback-query]}])
        feedback-raw (get-in result [:job :job/employer :employer/feedback :items])

        feedback (map (fn [feedback]
                        {:id (:message/id feedback)
                         :rating (:feedback/rating feedback)
                         :text (:feedback/text feedback)
                         :author (-> feedback :feedback/from-user :user/name)
                         :image-url (-> feedback :feedback/from-user :user/profile-image)})
                      feedback-raw)]
    [:div.feedback-listing
        [:div.label "Feedback for employer"]
        (if (empty? feedback)
          [:label "No feedback yet for this employer"]
          (into [c-carousel-old {}] (map #(c-feedback-slide %) feedback)))]))

(defmethod page :route.job/detail []
  (fn []
    (let [page-params (re/subscribe [:district.ui.router.subs/active-page-params])
          contract-address (:id @page-params)
          active-user (:user/id @(re/subscribe [:ethlance.ui.subscriptions/active-session]))
          job-query [:job {:job/id contract-address}
                     [:job/id
                      :job/title
                      :job/description
                      :job/required-skills
                      :job/category
                      :job/status
                      :job/required-experience-level
                      :job/required-availability
                      :job/bid-option
                      :job/estimated-project-length

                      :job/token-type
                      :job/token-amount
                      :job/token-address
                      :job/token-id

                      [:token-details [:token-detail/id
                                       :token-detail/name
                                       :token-detail/symbol]]
                      [:job/employer [:employer/rating
                                      :user/id
                                      [:user [:user/country
                                              :user/name
                                              :user/profile-image]]]]
                      [:job/arbiter [:arbiter/rating
                                     :arbiter/fee
                                     :arbiter/fee-currency-id
                                     :user/id
                                     [:user [:user/id
                                             :user/country
                                             :user/name
                                             :user/profile-image]]]]]]
          query-results (re/subscribe [::gql/query {:queries [job-query] :refetch-on :create-proposal-success}])
          results (:job @query-results)

          *title (:job/title results)
          *description (:job/description results)
          *sub-title (:job/category results)
          *experience (:job/required-experience-level results)
          *posted-time "Posted 7 Days Ago"
          *job-info-tags (remove nil? [(:job/estimated-project-length results)
                           (:job/status results)
                           (:job/required-experience-level results)
                           (:job/bid-option results)])
          *bid-option (:job/bid-option results)
          *required-skills (:job/required-skills results)

          *employer-name (get-in results [:job/employer :user :user/name])
          *employer-address (get-in results [:job/employer :user/id])
          *employer-rating (get-in results [:job/employer :employer/rating])
          *employer-country (get-in results [:job/employer :user :user/country])
          *employer-profile-image (get-in results [:job/employer :user :user/profile-image])

          *arbiter-name (get-in results [:job/arbiter :user :user/name])
          *arbiter-address (get-in results [:job/arbiter :user/id])
          *arbiter-rating (get-in results [:job/arbiter :arbiter/rating])
          *arbiter-country (get-in results [:job/arbiter :user :user/country])
          *arbiter-profile-image (get-in results [:job/arbiter :user :user/profile-image])
          *arbiter-fee (get-in results [:job/arbiter :arbiter/fee])
          *arbiter-fee-currency (-> (get-in results [:job/arbiter :arbiter/fee-currency-id])
                                    (or ,,, "")
                                    name
                                    clojure.string/upper-case)

          raw-token-amount (get-in results [:job/token-amount])
          *job-token-type (get-in results [:job/token-type])
          *job-token-id (get-in results [:job/token-id])
          *job-token-address (get-in results [:job/token-address])
          *job-token-amount (if (= (str *job-token-type) "eth")
                              (shared-utils/wei->eth raw-token-amount)
                              raw-token-amount)
          *token-detail-name (get-in results [:token-details :token-detail/name])
          *token-detail-symbol (get-in results [:token-details :token-detail/symbol])

          *proposal-token-amount (re/subscribe [:page.job-detail/proposal-token-amount])
          *proposal-text (re/subscribe [:page.job-detail/proposal-text])

          proposals (re/subscribe [:page.job-detail/active-proposals])
          my-proposal (re/subscribe [:page.job-detail/my-proposal])
          my-job-story-id (:job-story/id @my-proposal)
          my-proposal? (not (nil? @my-proposal))
          can-send-proposals? (and (not my-proposal?) (ilike!= active-user *employer-address))
          my-proposal-withdrawable? (and @my-proposal (= :proposal (:status @my-proposal)))]
      [c-main-layout {:container-opts {:class :job-detail-main-container}}
       [:div.header
        [:div.main
         [:div.title *title]
         [:div.sub-title *sub-title] ; TODO: where this comes from
         [:div.description *description]
         [:div.label "Required Skills"]
         [:div.skill-listing
          (for [skill *required-skills] [c-tag {:key skill} [c-tag-label skill]])]
         [:div.ticket-listing
         [:a.ticket {:href (token-utils/address->token-info-url *job-token-address) :target "_blank"}
          [:div.label "Available Funds"]
          [:div.amount (str *job-token-amount " " *token-detail-symbol " (" (or *token-detail-name *job-token-type) ")")]]]
         [:div.profiles
          [:a.employer-detail {:on-click (util.navigation/create-handler {:route :route.user/profile
                                                                          :params {:address *employer-address}
                                                                          :query {:tab :employer}})
                                 :href (util.navigation/resolve-route {:route :route.user/profile
                                                                       :params {:address *employer-address}
                                                                       :query {:tab :employer}})}
           [:div.header "Employer"]
           [:div.profile-image [c-profile-image {:src *employer-profile-image}]]
           [:div.name *employer-name]
           [:div.rating [c-rating {:rating *employer-rating}]]
           [:div.location *employer-country]
           [:div.fee ""]]
          [:a.arbiter-detail {:on-click (util.navigation/create-handler {:route :route.user/profile
                                                                         :params {:address *arbiter-address}
                                                                         :query {:tab :arbiter}})
                              :href (util.navigation/resolve-route {:route :route.user/profile
                                                                    :params {:address *arbiter-address}
                                                                    :query {:tab :arbiter}})}
           [:div.header "Arbiter"]
           [:div.profile-image [c-profile-image {:src *arbiter-profile-image}]]
           [:div.name *arbiter-name]
           [:div.rating [c-rating {:rating *arbiter-rating}]]
           [:div.location *arbiter-country]
           [:div.fee (str *arbiter-fee " " *arbiter-fee-currency)]]]]
        [:div.side
         [:div.label *posted-time]
         (for [tag-text *job-info-tags] [c-tag {:key tag-text} [c-tag-label tag-text]])]]
       [:div.proposal-listing
        [:div.label "Proposals"]
        [c-scrollable
         {:forceVisible true :autoHide false}
          (into [c-table {:headers ["" "Candidate" "Rate" "Created" "Status"]}]
                (map (fn [proposal]
                       [[:span (if (:current-user? proposal) "⭐" "")]
                        [:span (:candidate-name proposal)]
                        [:span (token-utils/human-amount (:rate proposal) *job-token-type)]
                        [:span (format/time-ago (new js/Date (:created-at proposal)))] ; TODO: remove new js/Date after switching to district.ui.graphql that converts Date GQL type automatically
                        [:span (:status proposal)]])
                     @proposals))]
        [:div.button-listing
         [c-circle-icon-button {:name :ic-arrow-left2 :size :small}]
         [c-circle-icon-button {:name :ic-arrow-left :size :small}]
         [c-circle-icon-button {:name :ic-arrow-right :size :small}]
         [c-circle-icon-button {:name :ic-arrow-right2 :size :small}]]

        [:div.proposal-form
         [:div.label "Send Proposal"]
         [c-token-values {:disabled? (not can-send-proposals?)
                          :token-type *job-token-type
                          :token-amount (if my-proposal? (:rate @my-proposal) @*proposal-token-amount)
                          :token-id *job-token-id
                          :token-address *job-token-address
                          :token-name *token-detail-name
                          :token-symbol *token-detail-symbol}]
         [:label "The amount is for payment type: " (str *bid-option)]
         [:div.description-input
          [c-textarea-input
           {:disabled (not can-send-proposals?)
            :placeholder "Proposal Description"
            :value (if my-proposal? (:message @my-proposal) @*proposal-text)
            :on-change #(re/dispatch [:page.job-detail/set-proposal-text %])}]]

         (if my-proposal-withdrawable?
           [c-button {:color :warning :on-click (fn [] (>evt [:page.job-proposal/remove my-job-story-id]))
                      :size :small}
            [c-button-label "Remove"]])
         (if (not my-proposal?)
           [c-button {:style (when (not can-send-proposals?) {:background :gray})
                      :on-click (fn []
                                  (when can-send-proposals? (>evt [:page.job-proposal/send contract-address])))
                      :size :small}
            [c-button-label "Send"]])]]

       [c-invoice-listing contract-address]

       [c-employer-feedback contract-address]])))
