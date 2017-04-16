(ns ethlance.pages.contract-detail-page
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [clojure.set :as set]
    [clojure.string :as string]
    [ethlance.components.message-bubble :refer [message-bubble]]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a]]
    [ethlance.components.star-rating :refer [star-rating]]
    [ethlance.constants :as constants]
    [ethlance.ethlance-db :as ethlance-db]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [goog.string :as gstring]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn get-employer [contract]
  (get-in contract [:contract/job :job/employer]))

(defn employer-first-name [contract]
  (u/butlast-word (:user/name (get-employer contract))))

(defn freelancer-first-name [contract]
  (u/butlast-word (:user/name (:contract/freelancer contract))))

(defn italic-description [text]
  [:div
   "• "
   [:span {:style styles/italic-text} text]])

(defn add-contract-message-form []
  (let [contract (subscribe [:contract/detail])
        form (subscribe [:form.message/add-job-contract-message])]
    (fn []
      (let [{:keys [:loading? :errors :data]} @form
            {:keys [:message/text]} data]
        [paper
         {:loading? loading?}
         [:h2 "Send Message"]
         [misc/textarea
          {:floating-label-text "Message"
           :form-key :form.message/add-job-contract-message
           :field-key :message/text
           :max-length-key :max-message-length
           :value text
           :hint-text misc/privacy-warning-hint}]
         [misc/send-button
          {:disabled (or loading?
                         (boolean (seq errors))
                         (empty? (string/trim text)))
           :on-touch-tap #(dispatch [:contract.message/add-job-contract-message
                                     (merge data (select-keys @contract [:contract/id]))])}]]))))

(defn contract-messages [{:keys [:contract/status]}]
  (let [messages (subscribe [:contract/messages status])
        contract (subscribe [:contract/detail])]
    (fn []
      (when (seq @messages)
        [:div
         (for [{:keys [:message/id :message/created-on :message/text :message/sender]} @messages]
           [message-bubble
            {:key id
             :side (if (= (:user/id (:contract/freelancer @contract)) (:user/id sender))
                     :left
                     :right)
             :user sender
             :date created-on}
            text])]))))

(defn tabs-with-message-form [tab-props body]
  [ui/tabs
   {:ink-bar-style {:display :none}}
   [ui/tab
    tab-props
    body]
   [ui/tab
    {:label "Send Message"}
    [add-contract-message-form]]])

(defn invitation-detail []
  (let [contract (subscribe [:contract/detail])]
    (fn []
      (let [{:keys [:invitation/created-on :invitation/description]} @contract]
        (when created-on
          (let [italic-text (gstring/format "%s invited %s to apply for the job"
                                            (employer-first-name @contract)
                                            (freelancer-first-name @contract))]
            [message-bubble
             {:side :right
              :user (get-employer @contract)
              :date created-on}
             [:div
              [italic-description italic-text]
              description]]))))))


(defn proposal-detail []
  (let [contract (subscribe [:contract/detail])]
    (fn []
      (let [{:keys [:proposal/description :proposal/created-on :proposal/rate
                    :contract/freelancer :contract/job]} @contract]
        (when created-on
          (let [{:keys [:job/payment-type :job/reference-currency]} job]
            (let [italic-text [:span (gstring/format "%s applied for the job with rate "
                                                     (freelancer-first-name @contract))
                               [:b [misc/rate rate payment-type {:value-currency reference-currency}]]]]
              [message-bubble
               {:side :left
                :user freelancer
                :date created-on}
               [:div
                [italic-description italic-text]
                description]])))))))

(defn contract-detail []
  (let [contract (subscribe [:contract/detail])]
    (fn []
      (let [{:keys [:contract/created-on :contract/description]} @contract]
        (when created-on
          (let [italic-text (gstring/format "%s hired %s!"
                                            (employer-first-name @contract)
                                            (freelancer-first-name @contract))]
            [message-bubble
             {:side :right
              :user (get-employer @contract)
              :date created-on}
             [:div
              [italic-description italic-text]
              description]]))))))

(defn contract-cancelled-detail []
  (let [contract (subscribe [:contract/detail])]
    (fn []
      (let [{:keys [:contract/cancelled-on :contract/cancel-description :contract/freelancer]} @contract]
        (when cancelled-on
          (let [italic-text (gstring/format "%s cancelled this contract"
                                            (freelancer-first-name @contract))]
            [message-bubble
             {:side :left
              :user freelancer
              :date cancelled-on}
             [:div
              [italic-description italic-text]
              cancel-description]]))))))

(defn invoices-link []
  (let [contract (subscribe [:contract/detail])]
    (fn []
      (let [{:keys [:contract/id :contract/invoices-count :contract/created-on]} @contract]
        (when created-on
          [row
           [col
            {:xs 12}
            [:h3
             [a
              {:route :contract/invoices
               :route-params {:contract/id id}}
              (str (freelancer-first-name @contract) " sent " invoices-count " "
                   (u/pluralize "invoice" invoices-count))]]]])))))

(defn feedback-italic-text [name done-by? rating]
  (gstring/format "%s%s left feedback with rating %s/5"
                  name
                  (if done-by? " ended contract and" "")
                  (u/rating->star rating)))

(defn freelancer-feedback [{:keys [:contract/freelancer-feedback :contract/freelancer-feedback-on
                                   :contract/freelancer-feedback-rating :contract/done-by-freelancer?]
                            :as contract}]
  (when freelancer-feedback-on
    (let [italic-text (feedback-italic-text (freelancer-first-name contract)
                                            done-by-freelancer?
                                            freelancer-feedback-rating)]
      [message-bubble
       {:side :left
        :user (:contract/freelancer contract)
        :date freelancer-feedback-on}
       [:div
        [italic-description italic-text]
        freelancer-feedback]])))

(defn employer-feedback [{:keys [:contract/employer-feedback :contract/employer-feedback-on
                                 :contract/employer-feedback-rating :contract/done-by-freelancer?]
                          :as contract}]
  (when employer-feedback-on
    (let [italic-text (feedback-italic-text (employer-first-name contract)
                                            (not done-by-freelancer?)
                                            employer-feedback-rating)]
      [message-bubble
       {:side :right
        :user (get-employer contract)
        :date employer-feedback-on}
       [:div
        [italic-description italic-text]
        employer-feedback]])))

(defn add-contract-form []
  (let [contract (subscribe [:contract/detail])
        form (subscribe [:form.contract/add-contract])]
    (fn []
      (let [{:keys [:contract/id]} @contract
            {:keys [:loading? :errors :data]} @form
            {:keys [:contract/description :contract/hiring-done?]} data]
        [tabs-with-message-form
         {:label "Accept Proposal"}
         [paper
          {:loading? loading?}
          [:h2 "Accept Proposal"]
          [misc/textarea
           {:floating-label-text "Message"
            :form-key :form.contract/add-contract
            :field-key :contract/description
            :max-length-key :max-contract-desc
            :value description
            :hint-text misc/privacy-warning-hint}]
          [ui/checkbox
           {:label "Close hiring for this job"
            :default-checked hiring-done?
            :style styles/form-item
            :on-check #(dispatch [:form/set-value :form.contract/add-contract :contract/hiring-done? %2])}]
          [misc/send-button
           {:disabled (or loading? (boolean (seq errors)))
            :label "Hire"
            :on-touch-tap #(dispatch [:contract.contract/add-contract (merge data {:contract/id id})])}]]]))))

(defn cancel-contract-form []
  (let [contract (subscribe [:contract/detail])
        active-user-id (subscribe [:db/active-address])
        form (subscribe [:form.contract/cancel-contract])]
    (fn []
      (let [{:keys [:contract/id]} @contract
            {:keys [:loading? :errors :data]} @form
            {:keys [:contract/cancel-description]} data]
        [tabs-with-message-form
         {:label "Cancel Contract"}
         [paper
          {:loading? loading?}
          [:h2 "Cancel Contract"]
          [:div {:style (merge styles/fade-text styles/margin-top-gutter-less)}
           "You can cancel this contract in case you decided for another job or you can't start work for other reasons."
           [:br] "After you create at least one invoice for this contract, this option will be unavailable."]
          [misc/textarea
           {:floating-label-text "Message"
            :form-key :form.contract/cancel-contract
            :field-key :contract/cancel-description
            :max-length-key :max-contract-desc
            :min-length-key :min-contract-desc
            :hint-text misc/privacy-warning-hint
            :value cancel-description}]
          [misc/send-button
           {:disabled (or loading? (boolean (seq errors)))
            :label "Cancel Contract"
            :on-touch-tap #(dispatch [:contract.contract/cancel-contract (merge data {:contract/id id})])}]]]))))

(defn add-feedback-form []
  (let [contract (subscribe [:contract/detail])
        active-user-id (subscribe [:db/active-address])
        form (subscribe [:form.contract/add-feedback])]
    (fn []
      (let [{:keys [:contract/status :contract/job :contract/id :contract/invoices-count]} @contract
            {:keys [:job/employer]} job
            {:keys [:loading? :errors :data]} @form
            {:keys [:contract/feedback :contract/feedback-rating]} data
            employer? (= (:user/id employer) @active-user-id)]
        [tabs-with-message-form
         {:label "Leave Feedback"}
         [paper
          {:loading? loading?}
          [:h2 "Leave Feedback"]
          [row-plain
           {:bottom "xs"}
           [star-rating
            {:star-count 10
             :value (u/rating->star feedback-rating)
             :on-star-click #(dispatch [:form/set-value :form.contract/add-feedback :contract/feedback-rating
                                        (u/star->rating %1)])
             :style (merge styles/form-item
                           {:display :inline-block})}]
           [:span
            {:style styles/feedback-form-star-numbers}
            (u/rating->star feedback-rating) "/5"]]
          [misc/textarea
           {:floating-label-text "Feedback"
            :form-key :form.contract/add-feedback
            :field-key :contract/feedback
            :max-length-key :max-feedback
            :min-length-key :min-feedback
            :value feedback}]
          (when (= status 3)
            [:div {:style styles/form-item}
             (if (and (zero? invoices-count) employer?)
               "You will be able to leave feedback after the freelancer sends you at least one invoice."
               "Note that by leaving feedback you will end this contract, which means no more invoices can be sent.")])
          [misc/send-button
           {:disabled (or loading?
                          (boolean (seq errors))
                          (and employer? (zero? invoices-count)))
            :label "Send Feedback"
            :on-touch-tap #(dispatch [:contract.contract/add-feedback (merge data {:contract/id id})])}]]]))))

(defn submit-form []
  (let [show-add-contract-form? (subscribe [:contract/show-add-contract-form?])
        show-add-feedback-form? (subscribe [:contract/show-add-feedback-form?])
        show-cancel-contract-form? (subscribe [:contract/show-cancel-contract-form?])
        show-add-contract-message-form? (subscribe [:contract/show-add-contract-message-form?])]
    (fn []
      (cond
        @show-add-contract-form? [add-contract-form]
        @show-cancel-contract-form? [cancel-contract-form]
        @show-add-feedback-form? [add-feedback-form]
        @show-add-contract-message-form? [add-contract-message-form]
        :else nil))))

(defn contract-detail-page []
  (let [contract (subscribe [:contract/detail])
        contract-id (subscribe [:contract/route-contract-id])
        messages-loading? (subscribe [:contract/messages-loading?])]
    (fn []
      (let [{:keys [:contract/job :contract/done-by-freelancer? :contract/id]} @contract
            {:keys [:job/employer :job/title]} job]
        [misc/call-on-change
         {:load-on-mount? true
          :args @contract-id
          :on-change (fn []
                       (dispatch [:after-eth-contracts-loaded
                                  [:contract.db/load-contracts
                                   (set/union ethlance-db/contract-entity-fields
                                              #{:job/employer
                                                :user/name
                                                :user/gravatar})
                                   [@contract-id]]])
                       (dispatch [:after-eth-contracts-loaded
                                  [:contract.db/load-contracts
                                   (set/union
                                     ethlance-db/invitation-entity-fields
                                     ethlance-db/proposal-entity-fields)
                                   [@contract-id]]])
                       (dispatch [:after-eth-contracts-loaded
                                  [:contract.db/load-contracts
                                   (set/union
                                     ethlance-db/employer-feedback-entity-fields
                                     ethlance-db/freelancer-feedback-entity-fields)
                                   [@contract-id]]])
                       (dispatch [:after-eth-contracts-loaded
                                  [:contract.views/load-contract-messages {:contract/id @contract-id}]]))}
         [misc/center-layout
          [paper
           {:loading? (or (empty? (:user/name employer))
                          (and (not (:proposal/created-on @contract))
                               (not (:invitation/created-on @contract)))
                          @messages-loading?)
            :style styles/paper-section-main}
           (when (:user/name employer)
             [:div
              [:div
               [:h1
                "Job Proposal"]
               [:h3 [a {:route :job/detail
                        :route-params {:job/id (:job/id job)}}
                     title]]]
              [invoices-link]
              [:div {:style {:margin-bottom 20}}]
              [invitation-detail]
              [contract-messages
               {:contract/status 1}]
              [proposal-detail]
              [contract-messages
               {:contract/status 2}]
              [contract-detail]
              [contract-messages
               {:contract/status 3}]
              [contract-cancelled-detail]
              (if done-by-freelancer?
                [:div
                 [freelancer-feedback @contract]
                 [employer-feedback @contract]]
                [:div
                 [employer-feedback @contract]
                 [freelancer-feedback @contract]])])]
          (when (:user/name employer)
            [submit-form])]]))))
