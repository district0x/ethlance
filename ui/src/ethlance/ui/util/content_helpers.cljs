(ns ethlance.ui.util.content-helpers
  (:require
    [clojure.string]))

(defn str->paragraphs [s]
  (clojure.string/split-lines s))

(defn page-with-title [title content-str]
  [:div
   [:h2 {:class :title} title]
   (into [:div {:class :description :style {:white-space "pre-wrap"}}]
         (->> content-str
              str->paragraphs
              (map clojure.string/trim ,,,)
              (map #(conj [:p] %) ,,,)))])
