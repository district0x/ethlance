(ns ethlance.pages.freelancer-contracts-page
  (:require
    [cljs-react-material-ui.icons :as icons]
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.contracts-table :refer [contracts-table]]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a center-layout]]
    [ethlance.ethlance-db :as ethlance-db]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [re-frame.core :refer [subscribe dispatch]]
    ))

(defn freelancer-invitations [{:keys [:user/id]}]
  [contracts-table
   {:list-subscribe [:list/contracts :list/freelancer-invitations]
    :show-invitation? true
    :show-employer? true
    :show-job? true
    :initial-dispatch {:list-key :list/freelancer-invitations
                       :fn-key :ethlance-views/get-freelancer-contracts
                       :load-dispatch-key :contract.db/load-contracts
                       :schema (select-keys ethlance-db/proposal+invitation-schema
                                            [:contract/job :invitation/created-on])
                       :args {:user/id id :contract/status 1 :job/status 1}}
    :all-ids-subscribe [:list/ids :list/freelancer-invitations]
    :title "Invitations"
    :no-items-text "You have no job invitations"}])

(defn freelancer-proposals [{:keys [:user/id]}]
  [contracts-table
   {:list-subscribe [:list/contracts :list/freelancer-proposals]
    :show-proposal? true
    :show-rate? true
    :show-job? true
    :initial-dispatch {:list-key :list/freelancer-proposals
                       :fn-key :ethlance-views/get-freelancer-contracts
                       :load-dispatch-key :contract.db/load-contracts
                       :schema (select-keys ethlance-db/proposal+invitation-schema
                                            [:contract/job :proposal/created-on :proposal/rate])
                       :args {:user/id id :contract/status 2 :job/status 1}}
    :all-ids-subscribe [:list/ids :list/freelancer-proposals]
    :title "Pending Proposals"
    :no-items-text "You have no pending job proposals"}])

(defn freelancer-contracts-open [{:keys [:user/id]}]
  [contracts-table
   {:list-subscribe [:list/contracts :list/freelancer-contracts-open]
    :show-contract? true
    :show-rate? true
    :show-total-paid? true
    :show-job? true
    :initial-dispatch {:list-key :list/freelancer-contracts-open
                       :fn-key :ethlance-views/get-freelancer-contracts
                       :load-dispatch-key :contract.db/load-contracts
                       :schema (select-keys ethlance-db/contract-all-schema
                                            [:contract/job :contract/created-on :proposal/rate :contract/total-paid])
                       :args {:user/id id :contract/status 3 :job/status 0}}
    :all-ids-subscribe [:list/ids :list/freelancer-contracts-open]
    :title "Active Contracts"
    :no-items-text "You have no active contracts"}])

(defn freelancer-contracts-done [{:keys [:user/id]}]
  [contracts-table
   {:list-subscribe [:list/contracts :list/freelancer-contracts-done]
    :show-contract? true
    :show-contract-done? true
    :show-rate? true
    :show-total-paid? true
    :show-job? true
    :initial-dispatch {:list-key :list/freelancer-contracts-done
                       :fn-key :ethlance-views/get-freelancer-contracts
                       :load-dispatch-key :contract.db/load-contracts
                       :schema (select-keys ethlance-db/contract-all-schema
                                            [:contract/job :contract/created-on :proposal/rate :contract/total-paid
                                             :contract/done-on])
                       :args {:user/id id :contract/status 4 :job/status 0}}
    :all-ids-subscribe [:list/ids :list/freelancer-contracts-done]
    :title "Past Contracts"
    :no-items-text "You have no past contracts"}])

(defn freelancer-contracts-page []
  (let [user (subscribe [:db/active-user])]
    (fn []
      [misc/freelancer-only-page
       [center-layout
        [freelancer-invitations @user]
        [freelancer-proposals @user]
        [freelancer-contracts-open @user]
        [freelancer-contracts-done @user]]])))
