(ns ethlance.components.user-sponsorships
  (:require
    [ethlance.ethlance-db :as ethlance-db]
    [ethlance.components.sponsorships-table :refer [sponsorships-table]]
    [re-frame.core :refer [subscribe dispatch]]))

(defn user-sponsorships [{:keys [:user/id]}]
  (let [xs-width? (subscribe [:window/xs-width?])]
    [sponsorships-table
     {:list-subscribe [:list/sponsorships :list/user-sponsorships]
      :show-job? true
      :show-refunded-amount? (not @xs-width?)
      :show-name? true
      :title "Sponsorships"
      :no-items-text "This user hasn't sponsored any job yet"
      :initial-dispatch {:list-key :list/user-sponsorships
                         :fn-key :ethlance-views/get-user-sponsorships
                         :load-dispatch-key :contract.db/load-sponsorships
                         :fields #{:sponsorship/amount :sponsorship/name :sponsorship/link :sponsorship/refunded?
                                   :sponsorship/refunded-amount :sponsorship/job :job/title}
                         :args {:user/id id}}
      :all-ids-subscribe [:list/ids :list/user-sponsorships]}]))
