(ns ethlance.pages.search-sponsorable-jobs-page
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.icons :as icons]
    [ethlance.components.list-pagination :refer [list-pagination]]
    [ethlance.components.misc :as misc :refer [col row paper-thin row-plain a currency]]
    [ethlance.components.search-results :refer [search-results-employer]]
    [ethlance.components.skills-chips :refer [skills-chips]]
    [ethlance.components.star-rating :refer [star-rating]]
    [ethlance.constants :as constants]
    [ethlance.ethlance-db :as ethlance-db]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn change-page [new-offset]
  (dispatch [:form.search/set-value :search/offset new-offset])
  (dispatch [:window/scroll-to-top]))

(defn info-item [title value]
  [:span " - " title ": " [:span
                           {:style styles/dark-text}
                           value]])

(defn search-results []
  (let [list (subscribe [:list/jobs :list/sponsorable-jobs])]
    (dispatch [:after-eth-contracts-loaded [:contract.views/get-sponsorable-jobs {}]])
    (fn []
      (let [{:keys [:loading? :items :offset :limit]} @list]
        [misc/paper
         {:loading? loading?}
         [:h1
          {:style styles/margin-bottom-gutter-less}
          "Jobs to Sponsor"]
         [:div
          {:style styles/paper-section-main}
          (if (seq items)
            (for [{:keys [:job/title :job/id :job/created-on :job/skills :job/skills-count :job/employer
                          :job/sponsorships-balance :job/sponsorships-count :job/sponsorships-total
                          :job/status] :as item} items]
              [:div {:key id}
               [:h2
                {:style styles/overflow-ellipsis}
                [a {:style styles/search-result-headline
                    :route :job/detail
                    :route-params {:job/id id}} title]]
               [:div {:style styles/job-info}
                [:span (u/time-ago created-on)]
                [info-item "Sponsors" sponsorships-count]
                [info-item "Sponsored" [misc/currency sponsorships-total]]
                [info-item "Balance" [misc/currency sponsorships-balance]]]
               [skills-chips
                {:selected-skills skills
                 :always-show-all? true}]
               [search-results-employer {:user (:job/employer item)}]
               [misc/hr-small]])
            (when-not loading?
              [row-plain
               {:center "xs"}
               [:h3 "Currently there are no jobs to sponsor"]]))]
         [row-plain
          {:end "xs"}
          [list-pagination
           {:offset offset
            :limit limit
            :all-ids-subscribe [:list/ids :list/sponsorable-jobs]
            :list-db-path [:list/sponsorable-jobs]
            :load-dispatch [:contract.db/load-jobs ethlance-db/get-sponsorable-jobs-fields]
            :scroll-to-top? true}]]]))))

(defn search-sponsorable-jobs-page []
  [misc/center-layout
   [search-results]])
