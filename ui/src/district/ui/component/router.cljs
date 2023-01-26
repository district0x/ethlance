(ns district.ui.component.router
  (:require
    [district.ui.component.page :refer [page]]
    [district.ui.router.subs :as subs]
    [re-frame.core :refer [subscribe]]))

(defn router []
  (let [active-page (subscribe [::subs/active-page])]
    (fn []
      (let [{:keys [:name :params :query]} @active-page]
        (when name
          ^{:key (str name params query)} [page name])))))