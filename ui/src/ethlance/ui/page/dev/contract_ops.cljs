(ns ethlance.ui.page.dev.contract-ops
  (:require [district.ui.component.page :refer [page]]
            [ethlance.ui.component.main-layout :refer [c-main-layout]]
            [district.ui.router.subs :as router.subs]))

(defn c-contract-call-form []
  [:div
   [:input {:type :text :value 42 :on-change #(fn [x] (println "text" x))}]])

(defmethod page :route.dev/contract-ops []
  (fn []
    [:div
     [:h1 "Contract operations"]
     [c-contract-call-form]]))
