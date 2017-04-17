(ns ethlance.components.country-auto-complete
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.constants :refer [countries]]
    [ethlance.styles :as styles]
    [medley.core :as medley]))

(defn country-auto-complete []
  (fn [{:keys [value] :as props}]
    [ui/auto-complete
     (merge
       {:floating-label-text "Country"
        :open-on-focus true
        :dataSource countries
        :filter (aget js/MaterialUI "AutoComplete" "caseInsensitiveFilter")
        :max-search-results 20
        :menu-props styles/chip-input-menu-props
        :search-text (if (or (not value) (zero? value))
                       ""
                       (nth countries (dec value)))}
       (dissoc props :value)
       (when-let [on-new-request (:on-new-request props)]
         {:on-new-request (fn [value index]
                            (on-new-request value (inc index)))
          :on-update-input (fn [text]
                             (when-not (seq text)
                               (on-new-request "" 0)))}))]))
