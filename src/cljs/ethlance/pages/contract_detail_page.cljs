(ns ethlance.pages.contract-detail-page
  (:require
    [cljs-react-material-ui.icons :as icons]
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a]]
    [ethlance.constants :as constants]
    [ethlance.ethlance-db :as ethlance-db]
    [ethlance.components.message-bubble :refer [message-bubble]]
    [ethlance.components.truncated-text :refer [truncated-text]]
    [ethlance.styles :as styles]
    [goog.string :as gstring]
    [ethlance.utils :as u]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    ))

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
  (let [italic-text (gstring/format "%s applied for the job with rate %s"
                                    (freelancer-first-name contract)
                                    (u/format-rate rate (:job/payment-type job)))]
    [message-bubble
     {:side :left
      :user freelancer
      :date created-on}
     [:div
      [italic-description italic-text]
      description]]))

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

(defn contract-detail-page []
  (let [contract (subscribe [:contract/detail])
        contract-id (subscribe [:contract/route-contract-id])]
    (dispatch [:contract/initiate-load :contract.db/load-contracts
               ethlance-db/contract-schema
               [@contract-id]])
    (dispatch [:contract/initiate-load :contract.db/load-contracts
               ethlance-db/proposal+invitation-schema
               [@contract-id]])
    (fn []
      (let [{:keys [:contract/job :contract/done-by-freelancer? :contract/id]} @contract
            {:keys [:job/employer :job/title]} job]
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
             [invoices-link @contract]
             (if done-by-freelancer?
               [:div
                [freelancer-feedback @contract]
                [employer-feedback @contract]]
               [:div
                [employer-feedback @contract]
                [freelancer-feedback @contract]])])]]))))