(ns ethlance.ui.util.navigation
  (:require
   [district.ui.router.events :as router.events]
   [re-frame.core :as re]))


(defn create-handler
  "Generate a re-frame dispatch function for buttons to navigate to other pages.

  # Keyword Parameters

  :route - key of the given route

  :params - Query Parameters for the generated url

  :query - I have no idea.

  # Notes

  - Routes can be found at `ethlance.shared.routes`

  - This is used primarily for creating handlers for the :on-click
  event in reagent components."
  [{:keys [route params query]}]
  (fn [event]
    (.preventDefault event)
    (re/dispatch [::router.events/navigate route params query])))


(defn resolve-route
  "Resolve a given route with the given params and query

   # Notes

   - Used to populate buttons with an :href"
  [{:keys [route params query]}]
  @(re/subscribe [:district.ui.router.subs/resolve route params query]))
