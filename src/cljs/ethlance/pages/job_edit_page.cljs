(ns ethlance.pages.job-edit-page
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.icons :as icons]
    [ethlance.components.job-form :refer [job-form]]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a center-layout]]
    [ethlance.constants :as constants]
    [ethlance.ethlance-db :as ethlance-db]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [re-frame.core :refer [subscribe dispatch]]))

(defn job-edit-page []
  (let [form (subscribe [:form.job/set-job])
        job-id (subscribe [:job/route-job-id])
        active-user-id (subscribe [:db/active-address])]
    (fn []
      [misc/only-registered
       [misc/only-employer
        [center-layout
         [misc/call-on-change
          {:args @job-id
           :load-on-mount? true
           :on-change (fn [job-id]
                        (dispatch [:form/clear-data :form.job/set-job])
                        (dispatch [:contract.db/load-jobs ethlance-db/job-entity-fields [job-id]]))}
          (let [employer-id (:job/employer (:data @form))]
            (if (or (= @active-user-id employer-id)
                    (not employer-id))
              [job-form (merge @form {:form-key :form.job/set-job})]
              [paper
               [row
                {:middle "xs" :center "xs"}
                "You are not allowed to edit this job"]]))]]]])))
