(ns ethlance.ui.page.job-contract
  (:require [district.parsers :refer [parse-int]]
            [district.ui.component.page :refer [page]]
            [district.ui.router.subs :as router.subs]
            [ethlance.ui.component.button :refer [c-button c-button-label]]
            [ethlance.ui.component.chat :refer [c-chat-log]]
            [ethlance.ui.component.main-layout :refer [c-main-layout]]
            [ethlance.ui.component.rating :refer [c-rating]]
            [ethlance.ui.component.tabular-layout :refer [c-tabular-layout]]
            [ethlance.ui.component.textarea-input :refer [c-textarea-input]]
            [re-frame.core :as re]))

(defn c-job-detail-table
  [{:keys []}]
  [:div.job-detail-table

   [:div.name "Status"]
   [:div.value "Active"]

   [:div.name "Funds Available"]
   [:div.value "12,900 SNT"]

   [:div.name "Employer"]
   [:div.value "Cyrus Karsan"]

   [:div.name "Candidate"]
   [:div.value "Clement Lesaege"]

   [:div.name "Arbiter"]
   [:div.value "Keegan Quigley"]])

(defn c-header-profile
  [{:keys []}]
  [:div.header-profile
   [:div.title "Job Contract"]
   [:div.job-name "Finality Labs Full Stack Developer"]
   [:div.job-details
    [c-job-detail-table {}]]])

(defn c-chat [_]
  [c-chat-log
   [{:user-type :candidate
     :text "Hi Johan. I’ve read the white paper and I can do the STEPS smart contract for 14 ETH and the ICO smart contract for 5 ETH.

I am a NY based senior blockchain developer who has done work for Consensys, Status, Gitcoin, Market Protocol, and several others. I am also a smart contract auditor at solidified.io. Please feel free to reach out directly at email@gmail.com"
     :details ["has sent job proposal" "($25/hr)"]
     :full-name "Brian Curran"
     :date-updated "3 Days Ago"}

    {:user-type :employer
     :text "Hi Cyrus, welcome on board!"
     :details ["Has hired Brian Curran"]
     :full-name "Clement Lesaege"
     :date-updated "2 Days Ago"}]])

(defn c-employer-options []
  [c-tabular-layout
   {:key "employer-tabular-layout"
    :default-tab 0}

   {:label "Send Message"}
   [:div.message-input-container
    [:div.label "Message"]
    [c-textarea-input {:placeholder ""}]
    [c-button {:color :primary} [c-button-label "Send Message"]]]

   {:label "Raise Dispute"}
   [:div.dispute-input-container
    [:div.label "Dispute"]
    [c-textarea-input {:placeholder ""}]
    [c-button {:color :primary} [c-button-label "Raise Dispute"]]]

   {:label "Leave Feedback"}
   [:div.feedback-input-container
    [:div.rating-input
     [c-rating {:rating 3 :on-change (fn [])}]]
    [:div.label "Feedback"]
    [c-textarea-input {:placeholder ""}]
    [:span.note "Note, by leaving feedback, you will end this contract, which means no more invoices can be sent."]
    [c-button {:color :primary} [c-button-label "Send Feedback"]]]])

(defn c-candidate-options []
  [c-tabular-layout
   {:key "employer-tabular-layout"
    :default-tab 0}

   {:label "Send Message"}
   [:div.message-input-container
    [:div.label "Message"]
    [c-textarea-input {:placeholder ""}]
    [c-button {:color :primary} [c-button-label "Send Message"]]]

   {:label "Raise Dispute"}
   [:div.dispute-input-container
    [:div.label "Dispute"]
    [c-textarea-input {:placeholder ""}]
    [c-button {:color :primary} [c-button-label "Raise Dispute"]]]

   {:label "Leave Feedback"}
   [:div.feedback-input-container
    [:div.rating-input
     [c-rating {:rating 3 :on-change (fn [])}]]
    [:div.label "Feedback"]
    [c-textarea-input {:placeholder ""}]
    [:span.note "Note, by leaving feedback, you will end this contract, which means no more invoices can be sent."]
    [c-button {:color :primary} [c-button-label "Send Feedback"]]]])

(defn c-arbiter-options []
  [c-tabular-layout
   {:key "employer-tabular-layout"
    :default-tab 0}

   {:label "Send Message"}
   [:div.message-input-container
    [:div.label "Message"]
    [c-textarea-input {:placeholder ""}]
    [c-button {:color :primary} [c-button-label "Send Message"]]]

   {:label "Resolve Dispute"
    :active? true} ;; TODO: conditionally show
   [:div.dispute-input-container
    [:div.label "Dispute"]
    [c-textarea-input {:placeholder ""}]
    [c-button {:color :primary} [c-button-label "Resolve Dispute"]]]

   {:label "Leave Feedback"}
   [:div.feedback-input-container
    [:div.rating-input
     [c-rating {:rating 3 :on-change (fn [])}]]
    [:div.label "Feedback"]
    [c-textarea-input {:placeholder ""}]
    [:span.note "Note, by leaving feedback, you will end this contract, which means no more invoices can be sent."]
    [c-button {:color :primary} [c-button-label "Send Feedback"]]]])

(defn c-guest-options [])

(defmethod page :route.job/contract []
  (let [*active-page-params (re/subscribe [::router.subs/active-page-params])]
    (fn []
      (let [job-id (-> @*active-page-params :id parse-int)
            job-story-query
            @(re/subscribe
              [:gql/query
               {:queries
                [[:job-story
                  {:job/id job-id}
                  [:job/id]]]}])
            {job-story :job-story} job-story-query]
        [c-main-layout {:container-opts {:class :job-contract-main-container}}
         [:div.header-container
          [c-header-profile job-story]
          [c-chat job-story]]

         ;; TODO: query for signed-in user's relation to the contract (guest, candidate, employer, arbiter)
         [:div.options-container
          [c-employer-options]]]))))
