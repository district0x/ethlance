(ns ethlance.ui.effects
  (:require
    [cljs-web3.core :as web3]
    [district.ui.graphql.events :as gql-events]
    [ethlance.ui.events]
    [re-frame.core :as re]))


;; TODO : move this to maybe re-frame-web3-fx
(re/reg-fx
  :web3/personal-sign
  (fn [{:keys [web3 data-str from on-success on-error]}]
    (.sendAsync ^js (web3/current-provider web3)
                (clj->js {:method "personal_sign"
                          :params [data-str from]
                          :from from})
                (fn [err result]
                  (if err
                    (re/dispatch (conj on-error err))
                    (re/dispatch (conj on-success (aget result "result"))))))))


(re/reg-fx
  :data/upload
  (fn [{:keys [:data :on-success :on-error]}]
    (re/dispatch [::gql-events/mutation
                  {:queries [[:upload-data {:data (if (string? data) data (pr-str data))}]]
                   :on-success [:ethlance/data-upload-success on-success]
                   :on-error on-error}])))
