(ns ethlance.ui.page.home.events
  (:require
    [district.ui.web3-accounts.queries :as accounts-queries]
    [ethlance.shared.utils]
    [re-frame.core :as re]))


(defn logged-in?
  [active-account active-session-user-id]
  (and
    (not-any? nil? [active-account active-session-user-id])
    (ethlance.shared.utils/ilike= active-account active-session-user-id)))


(defn initialize-page
  "Event FX Handler. Setup listener to dispatch an event when the page is active/visited."
  [{:keys [db]} _]
  (when-not (logged-in? (accounts-queries/active-account db)
                        (get-in db [:active-session :user/id]))
    {:fx [[:dispatch [:modal/open ::sign-in]]]}))


(re/reg-event-fx :page.home/initialize-page initialize-page)
