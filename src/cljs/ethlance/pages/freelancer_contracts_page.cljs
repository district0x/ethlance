(ns ethlance.pages.freelancer-contracts-page
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.contracts-table :refer [contracts-table]]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a center-layout]]
    [ethlance.ethlance-db :as ethlance-db]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [re-frame.core :refer [subscribe dispatch]]
    ))

(defn freelancer-invitations []
  (let [xs-width? (subscribe [:window/xs-width?])]
    (fn [{:keys [:user/id]}]
      [contracts-table
       {:list-subscribe [:list/contracts :list/freelancer-invitations]
        :show-invitation? true
        :show-employer? (not @xs-width?)
        :show-job? true
        :initial-dispatch {:list-key :list/freelancer-invitations
                           :fn-key :ethlance-views/get-freelancer-contracts
                           :load-dispatch-key :contract.db/load-contracts
                           :fields #{:contract/job :invitation/created-on}
                           :args {:user/id id :contract/statuses [1] :job/statuses [1]}}
        :all-ids-subscribe [:list/ids :list/freelancer-invitations]
        :title "Invitations"
        :no-items-text "You have no job invitations"}])))

(defn freelancer-proposals [{:keys [:user/id]}]
  [contracts-table
   {:list-subscribe [:list/contracts :list/freelancer-proposals]
    :show-proposal? true
    :show-rate? true
    :show-job? true
    :initial-dispatch {:list-key :list/freelancer-proposals
                       :fn-key :ethlance-views/get-freelancer-contracts
                       :load-dispatch-key :contract.db/load-contracts
                       :fields #{:contract/job :proposal/created-on :proposal/rate}
                       :args {:user/id id :contract/statuses [2] :job/statuses [1]}}
    :all-ids-subscribe [:list/ids :list/freelancer-proposals]
    :title "Pending Proposals"
    :no-items-text "You have no pending job proposals"}])

(defn freelancer-contracts-open []
  (let [xs-width? (subscribe [:window/xs-width?])]
    (fn [{:keys [:user/id]}]
      [contracts-table
       {:list-subscribe [:list/contracts :list/freelancer-contracts-open]
        :show-created-on? true
        :show-rate? (not @xs-width?)
        :show-total-paid? true
        :show-job? true
        :initial-dispatch {:list-key :list/freelancer-contracts-open
                           :fn-key :ethlance-views/get-freelancer-contracts
                           :load-dispatch-key :contract.db/load-contracts
                           :fields #{:contract/job :contract/created-on :proposal/rate :contract/total-paid}
                           :args {:user/id id :contract/statuses [3] :job/statuses []}}
        :all-ids-subscribe [:list/ids :list/freelancer-contracts-open]
        :title "Active Contracts"
        :no-items-text "You have no active contracts"}])))

(defn freelancer-contracts-done []
  (let [xs-width? (subscribe [:window/xs-width?])]
    (fn [{:keys [:user/id]}]
      [contracts-table
       {:list-subscribe [:list/contracts :list/freelancer-contracts-done]
        :show-created-on? (not @xs-width?)
        :show-done-on? true
        :show-rate? (not @xs-width?)
        :show-total-paid? true
        :show-job? true
        :initial-dispatch {:list-key :list/freelancer-contracts-done
                           :fn-key :ethlance-views/get-freelancer-contracts
                           :load-dispatch-key :contract.db/load-contracts
                           :fields #{:contract/job
                                     :contract/created-on
                                     :proposal/rate
                                     :contract/total-paid
                                     :contract/done-on}
                           :args {:user/id id :contract/statuses [4] :job/statuses []}}
        :all-ids-subscribe [:list/ids :list/freelancer-contracts-done]
        :title "Past Contracts"
        :no-items-text "You have no past contracts"}])))

(defn freelancer-contracts-cancelled []
  (let [xs-width? (subscribe [:window/xs-width?])]
    (fn [{:keys [:user/id]}]
      [contracts-table
       {:list-subscribe [:list/contracts :list/freelancer-contracts-cancelled]
        :show-created-on? true
        :show-cancelled-on? true
        :show-job? true
        :initial-dispatch {:list-key :list/freelancer-contracts-cancelled
                           :fn-key :ethlance-views/get-freelancer-contracts
                           :load-dispatch-key :contract.db/load-contracts
                           :fields #{:contract/job
                                     :contract/created-on
                                     :contract/cancelled-on}
                           :args {:user/id id :contract/statuses [5] :job/statuses []}}
        :all-ids-subscribe [:list/ids :list/freelancer-contracts-cancelled]
        :title "Cancelled Contracts"
        :no-items-text "You have no cancelled contracts"}])))

(defn freelancer-contracts-page []
  (let [user (subscribe [:db/active-user])]
    (fn []
      [misc/only-registered
       [misc/only-freelancer
        [center-layout
         [freelancer-invitations @user]
         [freelancer-proposals @user]
         [freelancer-contracts-open @user]
         [freelancer-contracts-done @user]
         [freelancer-contracts-cancelled @user]]]])))
