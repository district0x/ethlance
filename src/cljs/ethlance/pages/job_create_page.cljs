(ns ethlance.pages.job-create-page
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

(defn job-create-page []
  (let [form (subscribe [:form.job/add-job])]
    (fn []
      [misc/only-registered
       [misc/only-employer
        [center-layout
         [job-form (merge @form {:form-key :form.job/add-job})]]]])))
