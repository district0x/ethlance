(ns ethlance.components.country-auto-complete
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [medley.core :as medley]
    [ethlance.constants :refer [countries]]))

(defn country-auto-complete []
  (fn [{:keys [value] :as props}]
    [ui/auto-complete
     (merge
       {:floating-label-text "Country"
        :open-on-focus true
        :dataSource countries
        :filter (aget js/MaterialUI "AutoComplete" "caseInsensitiveFilter")
        :max-search-results 5
        :search-text (if (or (not value) (zero? value))
                       ""
                       (nth countries (dec value)))}
       props
       (when-let [on-new-request (:on-new-request props)]
         {:on-new-request (fn [value index]
                            (on-new-request value (inc index)))
          :on-update-input (fn [text]
                             (when-not (seq text)
                               (on-new-request "" 0)))}))]))
