(ns ethlance.components.truncated-text
  (:require
    [cljsjs.react-truncate]
    [cljs-react-material-ui.reagent :as ui]
    [cljs-react-material-ui.icons :as icons]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [reagent.core :as r]
    [clojure.string :as string]
    [medley.core :as medley]
    [clojure.walk :as walk]))

(def react-truncate (r/adapt-react-class js/ReactTruncate))

(defn new-lines->br [text]
  (let [lines-indexed (medley/indexed (string/split text #"\n"))]
    (map (fn [[i line]]
           (let [line (r/as-element [:span {:key i} line])]
             (if (= i (dec (count lines-indexed)))
               line
               (clj->js [line (r/as-element [:br {:key (str i "br")}])]))))
         lines-indexed)))

(defn more-button [{:keys [:color] :as props}]
  [:span (r/merge-props
           {:style (merge styles/more-text
                          (when color
                            {:color color}))}
           (dissoc props :color))
   "...Read More"])

(defn truncated-text []
  (let [open? (r/atom false)]
    (fn [props & children]
      (let [[{:keys [:more-text-color :allow-whitespace? :lines]
              :as props} children] (u/parse-props-children props children)
            text (first children)]
        (if (or @open? (not (pos? lines)))
          [:div
           (when allow-whitespace? {:style styles/allow-whitespaces})
           text]
          [react-truncate
           {:lines lines
            :on-truncate (fn [truncated?]
                           (when-not truncated?
                             (reset! open? true)))
            :ellipsis (r/as-element [more-button {:color more-text-color
                                                  :on-click #(reset! open? true)}])}
           (new-lines->br text)])))))