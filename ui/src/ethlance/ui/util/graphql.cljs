(ns ethlance.ui.util.graphql)


(defn prepare-search-params
  [page-state search-fields]
  (reduce (fn [acc [filter-key & transformers]]
            (let [filter-val (reduce #(%2 %1)
                                     (get page-state filter-key)
                                     (or transformers []))]
              (if (or (nil? filter-val) ; Don't add nil or empty collections to the search
                      (and (sequential? filter-val)
                           (empty? filter-val)))
                acc
                (assoc acc filter-key filter-val))))
          {}
          search-fields))
