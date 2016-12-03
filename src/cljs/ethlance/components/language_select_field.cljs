(ns ethlance.components.language-select-field
  (:require [cljs-react-material-ui.reagent :as ui]
            [ethlance.constants :refer [languages]]))

(defn language-select-field []
  (fn [{:keys [value] :as props}]
    [ui/auto-complete
     (merge
       {:floating-label-text "Language"
        :open-on-focus true
        :dataSource languages
        :filter (aget js/MaterialUI "AutoComplete" "caseInsensitiveFilter")
        :search-text (if (or (not value) (zero? value))
                       ""
                       (nth languages (dec value)))
        :list-style {:max-height 300}}
       props
       (when-let [on-new-request (:on-new-request props)]
         {:on-new-request (fn [value index]
                            (on-new-request value (inc index)))
          :on-update-input (fn [text]
                             (when-not (seq text)
                               (on-new-request "" 0)))}))]))
