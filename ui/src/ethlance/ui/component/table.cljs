(ns ethlance.ui.component.table)


(defn c-table
  "Ethlance Table Component.

  This Component simplifies table construction.

  # Keyword Arguments and Optional Arguments

  :headers - Is a collection of DOM Elements (not limited to strings)
  which constitutes the <th> set of data elements. This number of
  elements that make up the header determines the number of elements
  that must be contained within each row.

  & rows - Each row has the same number of elements as the `:header` opt-arg.

  # Examples

  [c-table
   {:headers [\"Candidate\" \"Rate\" \"Created\" \"Status\"]}

   ;; Row 0
   [[:span \"Cyrus Keegan\"]
    [:span \"$25\"]
    [:span \"5 Days Ago\"]
    [:span \"Pending\"]]

   ;; Row 1
   [[:span \"Cyrus Keegan\"]
    [:span \"$25\"]
    [:span \"5 Days Ago\"]
    [:span \"Pending\"]]]

  ## Clickable (link) rows

  To have the whole row clickable, instead of passing nested vector, pass a vector of maps such that
  [
   ;; Row 0
   {:row-link {:href \"http://...\" :on-click (fn [] ..something that causes navigation... )}
    :row-cells [[:span \"First cell\" \"Second cell\" etc]]}
  ]
  "
  [{:keys [headers empty-message] :or {empty-message "No data yet to show"}} & rows]
  [:div.ethlance-table
   [:table
    [:tbody
     [:tr
      (doall
        (for [[i header] (map-indexed vector headers)]
          ^{:key (str "header-" i)}
          [:th header]))]
     (if (empty? rows)
       [:tr
        [:td {:style {:text-align "center"} :col-span (count headers)} empty-message]]
       (doall
         (for [[i row] (map-indexed vector rows)]
           (if (map? row)

             ^{:key (str "row-" i)}
             [:tr.clickable (:row-link row)
              (for [[i elem] (map-indexed vector (:row-cells row))]
                ^{:key (str "elem-" i)}
                [:td elem])]

             ^{:key (str "row-" i)}
             [:tr
              (for [[i elem] (map-indexed vector row)]
                ^{:key (str "elem-" i)}
                [:td elem])]))))]]])
