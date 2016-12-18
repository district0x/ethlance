(ns ethlance.pages.skills-create-page
  (:require
    [cljs-react-material-ui.chip-input.reagent :as material-ui-chip-input]
    [cljs-react-material-ui.icons :as icons]
    [cljs-react-material-ui.reagent :as ui]
    [clojure.set :as set]
    [clojure.string :as string]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a center-layout]]
    [ethlance.constants :as constants]
    [ethlance.ethlance-db :as ethlance-db]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [goog.string :as gstring]
    [re-frame.core :refer [subscribe dispatch]]
    [cljs-web3.core :as web3]))

(defn add-error [error]
  (dispatch [:form/add-error :form.config/add-skills error]))

(defn remove-error [error]
  (dispatch [:form/remove-error :form.config/add-skills error]))

(defn add-skills-input []
  (let [skills (subscribe [:app/skills])
        form (subscribe [:form.config/add-skills])
        eth-config (subscribe [:eth/config])]
    (fn []
      (let [{:keys [:loading? :errors :data]} @form
            {:keys [:skill/names]} data
            existing-skill-names (set (map (comp string/lower-case :skill/name) (vals @skills)))
            max-skills-create-at-once (:max-skills-create-at-once @eth-config)]

        [paper
         {:loading? loading?}
         [:h2 "Add Skills"]
         [material-ui-chip-input/chip-input
          {:value names
           :full-width true
           :floating-label-text "New Skills"
           :hint-text "Type new skill and press Enter"
           :on-request-add (fn [skill-name]
                             (if-not (u/alphanumeric? skill-name)
                               (add-error :not-alphanumeric)
                               (do
                                 (remove-error :not-alphanumeric)
                                 (if (< 32 (count skill-name))
                                   (add-error :max-skill-name-length)
                                   (do
                                     (remove-error :max-skill-name-length)
                                     (if (contains? existing-skill-names (string/lower-case skill-name))
                                       (add-error :skill-already-exists)
                                       (do
                                         (remove-error :skill-already-exists)
                                         (let [skill-names (into [] (conj names skill-name))]
                                           (dispatch [:form/set-value :form.config/add-skills :skill/names skill-names])
                                           (when (< max-skills-create-at-once (count skill-names))
                                             (add-error :max-skills-create-at-once))))))))))
           :on-request-delete (fn [skill-name]
                                (remove-error :not-alphanumeric)
                                (remove-error :skill-already-exists)
                                (remove-error :max-skill-name-length)
                                (when (contains? (set names) skill-name)
                                  (let [skill-names (into [] (remove (partial = skill-name) names))]
                                    (dispatch [:form/set-value :form.config/add-skills :skill/names skill-names
                                               (comp pos? count)])
                                    (if (>= max-skills-create-at-once (count skill-names))
                                      (remove-error :max-skills-create-at-once)))))
           :error-text (cond
                         (contains? errors :not-alphanumeric)
                         "You can use only alphanumeric characters"

                         (contains? errors :max-skill-name-length)
                         "Maximum length is 32 characters"

                         (contains? errors :skill-already-exists)
                         "Such skill already exists"

                         (contains? errors :max-skills-create-at-once)
                         (gstring/format "You can add max %s skills at once" max-skills-create-at-once)
                         :else nil)}]
         [misc/send-button
          {:disabled (or loading? (boolean (seq (remove #{:not-alphanumeric :max-skill-name-length :skill-already-exists}
                                                        errors))))
           :on-touch-tap #(dispatch [:contract.config/add-skills data])}]]))))

(defn skills-create-page []
  [misc/only-registered
   [center-layout
    [add-skills-input]]])