(ns ethlance.pages.contract-detail-page
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.message-bubble :refer [message-bubble]]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a]]
    [ethlance.components.star-rating :refer [star-rating]]
    [ethlance.constants :as constants]
    [ethlance.ethlance-db :as ethlance-db]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [goog.string :as gstring]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [clojure.set :as set]))

(defn get-employer [contract]
  (get-in contract [:contract/job :job/employer]))

(defn employer-first-name [contract]
  (u/first-word (:user/name (get-employer contract))))

(defn freelancer-first-name [contract]
  (u/first-word (:user/name (:contract/freelancer contract))))

(defn italic-description [text]
  [:div
   {:style (merge styles/italic-text)}
   [:span text]])

(defn invitation-detail [{:keys [:invitation/created-on :invitation/description] :as contract}]
  (when created-on
    (let [italic-text (gstring/format "%s invited %s to apply for the job"
                                      (employer-first-name contract)
                                      (freelancer-first-name contract))]
      [message-bubble
       {:side :right
        :user (get-employer contract)
        :date created-on}
       [:div
        [italic-description italic-text]
        description]])))


(defn proposal-detail [{:keys [:proposal/description :proposal/created-on :proposal/rate
                               :contract/freelancer :contract/job]
                        :as contract}]
  (when created-on
    (let [{:keys [:job/payment-type :job/reference-currency]} job]
      (let [italic-text [:span (gstring/format "%s applied for the job with rate "
                                               (freelancer-first-name contract))
                         [:b [misc/rate rate payment-type {:value-currency reference-currency}]]]]
        [message-bubble
         {:side :left
          :user freelancer
          :date created-on}
         [:div
          [italic-description italic-text]
          description]]))))

(defn contract-detail [{:keys [:contract/created-on :contract/description] :as contract}]
  (when created-on
    (let [italic-text (gstring/format "%s hired %s!"
                                      (employer-first-name contract)
                                      (freelancer-first-name contract))]
      [message-bubble
       {:side :right
        :user (get-employer contract)
        :date created-on}
       [:div
        [italic-description italic-text]
        description]])))

(defn contract-cancelled-detail [{:keys [:contract/cancelled-on :contract/cancel-description
                                         :contract/freelancer] :as contract}]
  (when cancelled-on
    (let [italic-text (gstring/format "%s cancelled this contract"
                                      (freelancer-first-name contract))]
      [message-bubble
       {:side :left
        :user freelancer
        :date cancelled-on}
       [:div
        [italic-description italic-text]
        cancel-description]])))

(defn invoices-link [{:keys [:contract/id :contract/invoices-count :contract/created-on]
                      :as contract}]
  (when created-on
    [row
     {:center "xs"
      :style styles/contract-activity-row}
     [col
      {:xs 12}
      [ui/flat-button
       {:primary true
        :href (u/path-for :contract/invoices :contract/id id)
        :label (str (freelancer-first-name contract) " sent " invoices-count " "
                    (u/pluralize "invoice" invoices-count))}]]]))

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
        active-user-id (subscribe [:db/active-user-id])
        form (subscribe [:form.contract/add-contract])]
    (fn []
      (let [{:keys [:contract/status :contract/job :contract/id]} @contract
            {:keys [:job/employer]} job
            {:keys [:loading? :errors :data]} @form
            {:keys [:contract/description :contract/hiring-done?]} data]
        (when (and (= status 2) (= (:user/id employer) @active-user-id)
                   (= (:job/status job) 1))
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
             :on-touch-tap #(dispatch [:contract.contract/add-contract (merge data {:contract/id id})])}]])))))

(defn cancel-contract-form []
  (let [contract (subscribe [:contract/detail])
        active-user-id (subscribe [:db/active-user-id])
        form (subscribe [:form.contract/cancel-contract])]
    (fn []
      (let [{:keys [:contract/status :contract/id :contract/freelancer :contract/invoices-count]} @contract
            {:keys [:loading? :errors :data]} @form
            {:keys [:contract/cancel-description]} data]
        (when (and (= status 3)
                   (zero? invoices-count)
                   (= (:user/id freelancer) @active-user-id))
          [paper
           {:loading? loading?}
           [:h2 "Cancel Contract"]
           [:div {:style (merge styles/fade-text styles/margin-top-gutter-less)}
            "You can cancel this contract in case you decided for another job or you can't start work for other reasons."
            [:br] "After you create at least 1 invoice for this contract, this option won't be available anymore."]
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
             :on-touch-tap #(dispatch [:contract.contract/cancel-contract (merge data {:contract/id id})])}]])))))

(defn add-feedback-form []
  (let [contract (subscribe [:contract/detail])
        active-user-id (subscribe [:db/active-user-id])
        form (subscribe [:form.contract/add-feedback])]
    (fn []
      (let [{:keys [:contract/status :contract/job :contract/id :contract/freelancer
                    :contract/employer-feedback-on :contract/freelancer-feedback-on :contract/invoices-count]} @contract
            {:keys [:job/employer]} job
            {:keys [:loading? :errors :data]} @form
            {:keys [:contract/feedback :contract/feedback-rating]} data
            employer? (= (:user/id employer) @active-user-id)]
        (when (and (or (= status 3)
                       (= status 4))
                   (or (and employer?
                            (not employer-feedback-on))
                       (and (= (:user/id freelancer) @active-user-id)
                            (not freelancer-feedback-on)
                            (pos? invoices-count))))
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
                "You will be able to leave feedback only after freelancer sends you at least 1 invoice"
                "Note, by leaving feedback you will end this contract. That means no more invoices can be sent.")])
           [misc/send-button
            {:disabled (or loading?
                           (boolean (seq errors))
                           (and employer? (zero? invoices-count)))
             :on-touch-tap #(dispatch [:contract.contract/add-feedback (merge data {:contract/id id})])}]])))))

(defn contract-detail-page []
  (let [contract (subscribe [:contract/detail])
        contract-id (subscribe [:contract/route-contract-id])]
    (fn []
      (let [{:keys [:contract/job :contract/done-by-freelancer? :contract/id]} @contract
            {:keys [:job/employer :job/title]} job]
        [misc/call-on-change
         {:load-on-mount? true
          :args @contract-id
          :on-change (fn []
                       (dispatch [:after-eth-contracts-loaded
                                  [:contract.db/load-contracts
                                   (set/union ethlance-db/contract-entity-fields ethlance-db/feedback-entity-fields)
                                   [@contract-id]]])
                       (dispatch [:after-eth-contracts-loaded
                                  [:contract.db/load-contracts
                                   ethlance-db/proposal+invitation-entitiy-fields
                                   [@contract-id]]]))}
         [misc/center-layout
          [paper
           {:loading? (empty? (:user/name employer))
            :style styles/paper-section-main}
           (when (:user/name employer)
             [:div
              [:div
               {:style {:margin-bottom 60}}
               [:h1
                "Job Proposal"]
               [:h3 [a {:route :job/detail
                        :route-params {:job/id (:job/id job)}}
                     title]]]
              [invitation-detail @contract]
              [proposal-detail @contract]
              [contract-detail @contract]
              [contract-cancelled-detail @contract]
              [invoices-link @contract]
              (if done-by-freelancer?
                [:div
                 [freelancer-feedback @contract]
                 [employer-feedback @contract]]
                [:div
                 [employer-feedback @contract]
                 [freelancer-feedback @contract]])])]
          (when (:user/name employer)
            [:div
             [add-contract-form]
             [cancel-contract-form]
             [add-feedback-form]])]]))))