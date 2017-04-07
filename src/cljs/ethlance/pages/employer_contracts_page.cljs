(ns ethlance.pages.employer-contracts-page
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.contracts-table :refer [contracts-table]]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a center-layout]]
    [ethlance.ethlance-db :as ethlance-db]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [re-frame.core :refer [subscribe dispatch]]))

(defn employer-invitations []
  (let [xs-width? (subscribe [:window/xs-width?])]
    (fn [{:keys [:user/id]}]
      [contracts-table
       {:list-subscribe [:list/contracts :list/employer-invitations]
        :show-invitation? true
        :show-freelancer? true
        :show-job? true
        :initial-dispatch {:list-key :list/employer-invitations
                           :fn-key :ethlance-views/get-employer-contracts
                           :load-dispatch-key :contract.db/load-contracts
                           :fields #{:invitation/created-on
                                     :contract/job
                                     :contract/freelancer
                                     :job/title
                                     :user/name}
                           :args {:user/id id :contract/statuses [1] :job/statuses [1]}}
        :all-ids-subscribe [:list/ids :list/employer-invitations]
        :title "Invitations"
        :no-items-text "You have no pending job invitations"}])))

(defn employer-proposals [{:keys [:user/id]}]
  [contracts-table
   {:list-subscribe [:list/contracts :list/employer-proposals]
    :show-proposal? true
    :show-freelancer? true
    :show-rate? (not @(subscribe [:window/xs-width?]))
    :show-job? true
    :initial-dispatch {:list-key :list/employer-proposals
                       :fn-key :ethlance-views/get-employer-contracts
                       :load-dispatch-key :contract.db/load-contracts
                       :fields #{:proposal/created-on
                                 :proposal/rate
                                 :contract/job
                                 :contract/freelancer
                                 :job/title
                                 :job/payment-type
                                 :job/reference-currency
                                 :user/name}
                       :args {:user/id id :contract/statuses [2] :job/statuses [1]}}
    :all-ids-subscribe [:list/ids :list/employer-proposals]
    :title "Pending Proposals"
    :no-items-text "You have no pending job proposals"}])

(defn employer-contracts-open []
  (let [xs-width? (subscribe [:window/xs-width?])]
    (fn [{:keys [:user/id]}]
      [contracts-table
       {:list-subscribe [:list/contracts :list/employer-contracts-open]
        :show-rate? (not @xs-width?)
        :show-total-spent? true
        :show-job? true
        :show-freelancer? true
        :initial-dispatch {:list-key :list/employer-contracts-open
                           :fn-key :ethlance-views/get-employer-contracts
                           :load-dispatch-key :contract.db/load-contracts
                           :fields #{:proposal/rate
                                     :contract/job
                                     :contract/freelancer
                                     :contract/total-paid
                                     :job/title
                                     :job/payment-type
                                     :job/reference-currency
                                     :user/name}
                           :args {:user/id id :contract/statuses [3] :job/statuses []}}
        :all-ids-subscribe [:list/ids :list/employer-contracts-open]
        :title "Active Contracts"
        :no-items-text "You have no active contracts"}])))

(defn employer-contracts-done []
  (let [xs-width? (subscribe [:window/xs-width?])]
    (fn [{:keys [:user/id]}]
      [contracts-table
       {:list-subscribe [:list/contracts :list/employer-contracts-done]
        :show-done-on? (not @xs-width?)
        :show-freelancer? true
        :show-total-spent? true
        :show-job? true
        :initial-dispatch {:list-key :list/employer-contracts-done
                           :fn-key :ethlance-views/get-employer-contracts
                           :load-dispatch-key :contract.db/load-contracts
                           :fields #{:contract/done-on
                                     :contract/job
                                     :contract/freelancer
                                     :contract/total-paid
                                     :job/title
                                     :user/name}
                           :args {:user/id id :contract/statuses [4] :job/statuses []}}
        :all-ids-subscribe [:list/ids :list/employer-contracts-done]
        :title "Past Contracts"
        :no-items-text "You have no past contracts"}])))

(defn employer-contracts-cancelled []
  (let [xs-width? (subscribe [:window/xs-width?])]
    (fn [{:keys [:user/id]}]
      [contracts-table
       {:list-subscribe [:list/contracts :list/employer-contracts-cancelled]
        :show-freelancer? true
        :show-cancelled-on? true
        :show-job? true
        :initial-dispatch {:list-key :list/employer-contracts-cancelled
                           :fn-key :ethlance-views/get-employer-contracts
                           :load-dispatch-key :contract.db/load-contracts
                           :fields #{:contract/cancelled-on
                                     :contract/job
                                     :contract/freelancer
                                     :job/title
                                     :user/name}
                           :args {:user/id id :contract/statuses [5] :job/statuses []}}
        :all-ids-subscribe [:list/ids :list/employer-contracts-cancelled]
        :title "Cancelled Contracts"
        :no-items-text "You have no cancelled contracts"}])))

(defn employer-contracts-page []
  (let [user (subscribe [:db/active-user])]
    (fn []
      [misc/only-registered
       [misc/only-employer
        [center-layout
         [employer-invitations @user]
         [employer-proposals @user]
         [employer-contracts-open @user]
         [employer-contracts-done @user]
         [employer-contracts-cancelled @user]]]])))
