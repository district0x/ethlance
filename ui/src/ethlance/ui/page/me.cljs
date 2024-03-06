(ns ethlance.ui.page.me
  (:require
    [district.ui.component.page :refer [page]]
    [district.ui.graphql.subs :as gql]
    [district.ui.router.events :as router.events]
    [district.ui.router.subs :as router.subs]
    [ethlance.ui.component.info-message :refer [c-info-message]]
    [ethlance.ui.component.loading-spinner :refer [c-loading-spinner]]
    [ethlance.ui.component.main-layout :refer [c-main-layout]]
    [ethlance.ui.component.mobile-sidebar :refer [c-mobile-sidebar]]
    [ethlance.ui.component.pagination :refer [c-pagination-ends]]
    [ethlance.ui.component.table :refer [c-table]]
    [ethlance.ui.component.tabular-layout :refer [c-tabular-layout]]
    [ethlance.ui.util.dates :refer [formatted-date]]
    [ethlance.ui.util.navigation :refer [link-params] :as util.navigation]
    [ethlance.ui.util.tokens :as tokens]
    [re-frame.core :as re]))


(defn c-nav-sidebar-element
  [label id-value]
  (let [*active-page (re/subscribe [::router.subs/active-page])]
    (fn []
      (let [{active-page :name
             active-params :param
             active-query :query} @*active-page
            active-user (:user/id @(re/subscribe [:ethlance.ui.subscriptions/active-session]))
            default-active {"candidate" :my-candidate-contract-listing
                            "employer" :my-employer-job-listing
                            "arbiter" :my-arbiter-dispute-listing}
            query [:user {:user/id active-user}
                   [:user/is-registered-candidate
                    :user/is-registered-arbiter
                    :user/is-registered-employer]]

            results @(re/subscribe [::gql/query {:queries [query]}])
            *current-sidebar-choice (keyword (:sidebar active-query))
            user-role (some (fn [[role-name has-it?]]
                              (if has-it?
                                role-name
                                false))
                            [["candidate" (get-in results [:user :user/is-registered-candidate])]
                             ["employer" (get-in results [:user :user/is-registered-employer])]
                             ["arbiter" (get-in results [:user :user/is-registered-arbiter])]])
            tab-active-from-url? (= *current-sidebar-choice id-value)
            active-for-role-by-default? (and
                                          (= id-value (get default-active user-role))
                                          (not (contains? active-query :sidebar)))
            active? (if tab-active-from-url?
                      tab-active-from-url?
                      active-for-role-by-default?)
            updated-query (-> (or active-query {})
                              (assoc  :sidebar id-value)
                              (dissoc :tab))]
        [:div.nav-element
         [:a.link
          {:title (str "Navigate to '" label "'")
           :class [(when active? "active")]
           :href (util.navigation/resolve-route {:route active-page :params active-params :query updated-query})
           :on-click (util.navigation/create-handler {:route active-page :params active-params :query updated-query})}
          label]]))))


(defn c-table-listing
  "Produces tabl ewith headers

  Arguments:
    headers [{:title \"Column title\" :source <callable to get from rows seq map>}]
    rows    seq of maps

  Example:
    (c-table-listing [{:title \"Name\" :source :user/name}] [{:user/name \"John Doe\"}])"
  [headers rows & [link-params-fn paging]]

  (let [total-count (:total-count paging)
        limit (:limit paging)
        offset (:offset paging)]
    [:<>
     (if (= 0 total-count)
       [c-info-message "There are no items to show"]
       (into [c-table {:headers (map :title headers)}]
             (mapv (fn [row]
                     {:row-link (link-params (link-params-fn row))
                      :row-cells (mapv (fn [header]
                                         (if (nil? link-params-fn)
                                           [:span ((:source header) row)]
                                           [:a (link-params (link-params-fn row))
                                            [:span ((:source header) row)]]))
                                       headers)})
                   rows)))
     [c-pagination-ends
      {:total-count total-count
       :limit limit
       :offset offset
       :set-offset-event :page.me/set-pagination-offset}]]))


(defn tab-navigate-handler
  [sidebar tab]
  (fn []
    (re/dispatch [::router.events/navigate
                  :route.me/index
                  {}
                  {:sidebar sidebar :tab tab}])))


(defn spinner-until-data-ready
  [loading-states component-when-loading-finished]
  (if (not-every? false? loading-states)
    [c-loading-spinner]
    component-when-loading-finished))


(defn c-job-listing
  [user-type]
  (let [active-user (:user/id @(re/subscribe [:ethlance.ui.subscriptions/active-session]))
        url-query @(re/subscribe [::router.subs/active-page-query])
        tab (or (:tab url-query) "active")
        tab-to-index {"active" 0 "finished" 1}
        ;; Currently nothing sets the job status as finished (only job-story status)
        ;; Withdrawing all funds (on job details page) sets job to "ended" status
        ;; Alternatively the :status search param could accept array
        status-search-param (if (= tab "finished") "ended" tab)
        tab-index (get tab-to-index tab 0)
        limit @(re/subscribe [:page.me/pagination-limit])
        offset @(re/subscribe [:page.me/pagination-offset])
        job-query [:job-search {:search-params {user-type active-user
                                                :status status-search-param}
                                :limit limit
                                :offset offset
                                :order-by :date-created}
                   [:total-count
                    [:items [:job/id
                             :job/title
                             :job/status
                             :job/token-amount
                             :job/token-type
                             :job/date-created
                             [:arbitrations {:arbiter active-user}
                              [:total-count
                               [:items
                                [:arbitration/status]]]]
                             [:token-details [:token-detail/id
                                              :token-detail/name
                                              :token-detail/symbol
                                              :token-detail/decimals]]]]]]
        result @(re/subscribe [::gql/query {:queries [job-query]}])
        [loading? processing?] (map result [:graphql/loading? :graphql/preprocessing?])
        jobs (get-in result [:job-search :items])
        remuneration (fn [job]
                       (str (tokens/human-amount
                              (:job/token-amount job)
                              (:job/token-type job)
                              (get-in job [:token-details :token-detail/decimals]))
                            " " (-> job :token-details :token-detail/symbol)))
        arbitration-info (fn [job]
                           (let [arbitration-status (:arbitration/status (first (get-in job [:arbitrations :items])))]
                             (when arbitration-status (str "Arbitration: " arbitration-status))))
        jobs-table [{:title "Job Title" :source :job/title}
                    {:title "Remuneration" :source remuneration}
                    {:title "Created at" :source (partial formatted-date :job/date-created)}
                    {:title "Info" :source arbitration-info}]
        job-link-fn (fn [job] {:route :route.job/detail :params {:id (:job/id job)}})
        pagination {:total-count (get-in result [:job-search :total-count])
                    :limit limit
                    :offset offset}
        user->section {:employer :my-employer-job-listing
                       :creator :my-employer-job-listing
                       :arbiter :my-arbiter-job-listing}]
    [c-tabular-layout
     {:key "my-employer-job-tab-listing"
      :default-tab tab-index}

     {:label "Active Jobs" :on-click (tab-navigate-handler (user->section user-type) :active)}
     (spinner-until-data-ready
       [loading? processing?]
       [:div.listing.my-employer-job-listing
        [c-table-listing jobs-table jobs job-link-fn pagination]])

     {:label "Finished Jobs" :on-click (tab-navigate-handler (user->section user-type) :finished)}
     (spinner-until-data-ready
       [loading? processing?]
       [:div.listing.my-employer-job-listing
        [c-table-listing jobs-table jobs job-link-fn pagination]])]))


(defn c-my-employer-job-listing
  []
  [c-job-listing :creator])


(defn c-contract-listing
  [user-type user-address]
  (let [url-query @(re/subscribe [::router.subs/active-page-query])
        tab (or (:tab url-query) "invitation")
        tab-to-index {"invitation" 0 "proposal" 1 "active" 2 "finished" 3}
        tab-index (get tab-to-index tab 0)
        limit @(re/subscribe [:page.me/pagination-limit])
        offset @(re/subscribe [:page.me/pagination-offset])

        query [:job-story-search {:search-params {user-type user-address :status tab}
                                  :limit limit
                                  :offset offset
                                  :order-by :date-created}
               [:total-count
                [:items [:job/id
                         :job-story/id
                         :job-story/status
                         :job-story/date-created
                         :job-story/proposal-rate
                         [:candidate
                          [:user/id
                           [:user [:user/name]]]]
                         [:job [:job/title]]]]]]
        result @(re/subscribe [::gql/query {:queries [query]}])
        [loading? processing?] (map result [:graphql/loading? :graphql/preprocessing?])
        pagination {:total-count (get-in result [:job-story-search :total-count])
                    :limit limit
                    :offset offset}
        jobs (get-in result [:job-story-search :items])
        user-name-fn (fn [job] (get-in job [:candidate :user :user/name]))
        jobs-table [{:title "Job Title" :source #(get-in % [:job :job/title])}
                    {:title "Candidate" :source user-name-fn}
                    {:title "Created at" :source (partial formatted-date :job-story/date-created)}]
        contract-link-fn (fn [job] {:route :route.job/contract :params {:job-story-id (:job-story/id job)}})
        user->section {:employer :my-employer-contract-listing
                       :creator :my-employer-contract-listing
                       :candidate :my-candidate-contract-listing
                       :arbiter :my-arbiter-contract-listing}]
    [c-tabular-layout
     {:key "my-employer-job-tab-listing"
      :default-tab tab-index}

     {:label "Invitations" :on-click (tab-navigate-handler (user->section user-type) :invitation)}
     (spinner-until-data-ready
       [loading? processing?]
       [:div.listing.my-employer-job-listing
        [c-table-listing jobs-table jobs contract-link-fn pagination]])

     {:label "Pending Proposals" :on-click (tab-navigate-handler (user->section user-type) :proposal)}
     (spinner-until-data-ready
       [loading? processing?]
       [:div.listing.my-employer-job-listing
        [c-table-listing jobs-table jobs contract-link-fn pagination]])

     {:label "Active Contracts" :on-click (tab-navigate-handler (user->section user-type) :active)}
     (spinner-until-data-ready
       [loading? processing?]
       [:div.listing.my-employer-job-listing
        [c-table-listing jobs-table jobs contract-link-fn pagination]])

     {:label "Finished Contracts" :on-click (tab-navigate-handler (user->section user-type) :finished)}
     (spinner-until-data-ready
       [loading? processing?]
       [:div.listing.my-employer-job-listing
        [c-table-listing jobs-table jobs contract-link-fn pagination]])]))


(defn c-invoice-listing
  [user-type user-address]
  (let [url-query @(re/subscribe [::router.subs/active-page-query])
        tab (or (:tab url-query) "pending")
        tab-to-index {"pending" 0 "paid" 1}
        tab-index (get tab-to-index tab 0)
        limit @(re/subscribe [:page.me/pagination-limit])
        offset @(re/subscribe [:page.me/pagination-offset])

        query [:invoice-search {user-type user-address :status tab :limit limit :offset offset}
               [:total-count
                [:items
                 [:id
                  :job/id
                  [:job-story
                   [:job-story/id
                    [:job
                     [:job/id
                      :job/title
                      :job/token-type
                      [:token-details
                       [:token-detail/id
                        :token-detail/name
                        :token-detail/symbol
                        :token-detail/decimals]]]]]]
                  :invoice/status
                  :invoice/id
                  :invoice/amount-requested
                  :invoice/date-requested
                  :invoice/date-paid
                  :invoice/amount-paid
                  [:creation-message
                   [:message/id
                    :message/date-created
                    [:creator [:user/id
                               :user/name]]]]]]]]
        result @(re/subscribe [::gql/query {:queries [query]}])
        [loading? processing?] (map result [:graphql/loading? :graphql/preprocessing?])
        pagination {:total-count (get-in result [:invoice-search :total-count])
                    :limit limit
                    :offset offset}
        invoices (get-in result [:invoice-search :items])
        user-name-fn (fn [invoice] (get-in invoice [:creation-message :creator :user/name]))
        invoice-date-created-fn (partial formatted-date #(get-in % [:creation-message :message/date-created]))
        amount-requested-fn (fn [invoice]
                              (str (tokens/human-amount (:invoice/amount-requested invoice)
                                                        (get-in invoice [:job-story :job :job/token-type])
                                                        (get-in invoice [:job-story :job :token-details :token-detail/decimals]))
                                   " " (get-in invoice [:job-story :job :token-details :token-detail/symbol])))
        table [{:title "Job Title" :source #(get-in % [:job-story :job :job/title])}
               {:title "Candidate" :source user-name-fn}
               {:title "Amount Requested" :source amount-requested-fn}
               {:title "Created at" :source invoice-date-created-fn}
               {:title "Status" :source #(get % :invoice/status)}]
        invoice-link-fn (fn [invoice]
                          {:route :route.invoice/index
                           :params {:job-id (:job/id invoice) :invoice-id (:invoice/id invoice)}})
        user->section {:employer :my-employer-invoice-listing
                       :candidate :my-candidate-invoice-listing}]
    [c-tabular-layout
     {:key "my-employer-job-tab-listing"
      :default-tab tab-index}

     {:label "Pending Invoices" :on-click (tab-navigate-handler (user->section user-type) :pending)}
     (spinner-until-data-ready
       [loading? processing?]
       [:div.listing.my-employer-job-listing
        [c-table-listing table invoices invoice-link-fn pagination]])

     {:label "Paid Invoices" :on-click (tab-navigate-handler (user->section user-type) :paid)}
     (spinner-until-data-ready
       [loading? processing?]
       [:div.listing.my-employer-job-listing
        [c-table-listing table invoices invoice-link-fn pagination]])]))


(defn c-dispute-listing
  [user-type]
  (let [active-user (:user/id @(re/subscribe [:ethlance.ui.subscriptions/active-session]))
        url-query @(re/subscribe [::router.subs/active-page-query])
        tab (or (:tab url-query) "dispute-raised")
        tab-to-index {"dispute-raised" 0 "dispute-resolved" 1}
        tab-index (get tab-to-index tab 0)
        limit @(re/subscribe [:page.me/pagination-limit])
        offset @(re/subscribe [:page.me/pagination-offset])

        query [:dispute-search {user-type active-user :status tab :limit limit :offset offset}
               [:total-count
                [:items [:id
                         :invoice/id
                         :dispute/reason
                         :dispute/resolution
                         :dispute/date-created
                         :dispute/date-resolved
                         :dispute/status
                         :job-story/id

                         :invoice/amount-requested
                         :invoice/amount-paid
                         [:candidate
                          [[:user [:user/id :user/name]]]]
                         [:employer
                          [[:user [:user/id :user/name]]]]
                         [:arbiter
                          [[:user [:user/id :user/name]]]]
                         [:job
                          [:job/title
                           :job/token-type
                           [:token-details
                            [:token-detail/id
                             :token-detail/name
                             :token-detail/symbol
                             :token-detail/decimals]]]]]]]]
        disputes-result @(re/subscribe [::gql/query {:queries [query]}])
        [loading? processing?] (map disputes-result [:graphql/loading? :graphql/preprocessing?])
        pagination {:total-count (get-in disputes-result [:dispute-search :total-count])
                    :limit limit
                    :offset offset}
        disputes (get-in  disputes-result [:dispute-search :items])
        candidate-name-fn (fn [invoice] (get-in invoice [:candidate :user :user/name]))
        arbiter-name-fn (fn [invoice] (get-in invoice [:dispute-resolved-message :creator :user/name]))
        dispute-date-created-fn (partial formatted-date #(get % :dispute/date-created))
        dispute-date-resolved-fn (partial formatted-date #(get % :dispute/date-resolved))
        contract-link-fn (fn [dispute] {:route :route.job/contract :params {:job-story-id (:job-story/id dispute)}})
        amount-fn (fn [amount-source invoice]
                    (str (tokens/human-amount
                           (amount-source invoice)
                           (get-in invoice [:job :job/token-type])
                           (get-in invoice [:job :token-details :token-detail/decimals]))
                         " " (get-in invoice [:job :token-details :token-detail/symbol])))
        truncated-dispute-fn (fn [text-source invoice]
                               (let [text (get invoice text-source "")
                                     max-chars 20]
                                 (if (empty? text)
                                   ""
                                   (str (subs text 0 (min (count text) max-chars)) "..."))))
        open-table [{:title "Job Title" :source #(get-in % [:job :job/title])}
                    {:title "Candidate" :source candidate-name-fn}
                    {:title "Requested amount" :source (partial amount-fn :invoice/amount-requested)}
                    {:title "Reason" :source (partial truncated-dispute-fn :dispute/reason)}
                    {:title "Date raised" :source dispute-date-created-fn}]
        resolved-table [{:title "Job Title" :source #(get-in % [:job :job/title])}
                        {:title "Arbiter" :source arbiter-name-fn}
                        {:title "Resolution" :source (partial truncated-dispute-fn :dispute/resolution)}
                        {:title "Received amount" :source (partial amount-fn :invoice/amount-paid)}
                        {:title "Date resolved" :source dispute-date-resolved-fn}]
        user->section {:employer :my-employer-dispute-listing
                       :candidate :my-candidate-dispute-listing
                       :arbiter :my-arbiter-dispute-listing}]
    [c-tabular-layout
     {:key "my-employer-job-tab-listing"
      :default-tab tab-index}

     {:label "Open Disputes"
      :on-click (tab-navigate-handler (user->section user-type) :dispute-raised)}
     (spinner-until-data-ready
       [loading? processing?]
       [:div.listing.my-employer-job-listing
        [c-table-listing open-table disputes contract-link-fn pagination]])

     {:label "Resolved Disputes"
      :on-click (tab-navigate-handler (user->section user-type) :dispute-resolved)}
     (spinner-until-data-ready
       [loading? processing?]
       [:div.listing.my-employer-job-listing
        [c-table-listing resolved-table disputes contract-link-fn pagination]])]))


(defn c-my-employer-contract-listing
  []
  (let [active-user (:user/id @(re/subscribe [:ethlance.ui.subscriptions/active-session]))]
    [c-contract-listing :employer active-user]))


(defn c-my-employer-invoice-listing
  []
  (let [employer (:user/id @(re/subscribe [:ethlance.ui.subscriptions/active-session]))]
    [c-invoice-listing :employer employer]))


(defn c-my-employer-dispute-listing
  []
  (c-dispute-listing :employer))


;;
;; Candidate Sections
;;

(defn c-my-candidate-contract-listing
  []
  (let [active-user (:user/id @(re/subscribe [:ethlance.ui.subscriptions/active-session]))]
    [c-contract-listing :candidate active-user]))


(defn c-my-candidate-invoice-listing
  []
  (let [candidate (:user/id @(re/subscribe [:ethlance.ui.subscriptions/active-session]))]
    [c-invoice-listing :candidate candidate]))


(defn c-my-candidate-dispute-listing
  []
  [c-dispute-listing :candidate])


;;
;; Arbiter Sections
;;

(defn c-my-arbiter-job-listing
  []
  [c-job-listing :arbiter])


(defn c-my-arbiter-dispute-listing
  []
  [c-dispute-listing :arbiter])


(defn c-sidebar
  []
  [:div.sidebar
   [:div.section
    [:div.label "As Employer"]
    [c-nav-sidebar-element "My Jobs" :my-employer-job-listing]
    [c-nav-sidebar-element "My Contracts" :my-employer-contract-listing]
    [c-nav-sidebar-element "My Invoices" :my-employer-invoice-listing]
    [c-nav-sidebar-element "My Disputes" :my-employer-dispute-listing]]

   [:div.section
    [:div.label "As Candidate"]
    [c-nav-sidebar-element "My Contracts" :my-candidate-contract-listing]
    [c-nav-sidebar-element "My Invoices" :my-candidate-invoice-listing]
    [c-nav-sidebar-element "My Disputes" :my-candidate-dispute-listing]]

   [:div.section
    [:div.label "As Arbiter"]
    [c-nav-sidebar-element "My Jobs" :my-arbiter-job-listing]
    [c-nav-sidebar-element "My Disputes" :my-arbiter-dispute-listing]]])


(defn c-mobile-navigation
  []
  (fn []
    [c-mobile-sidebar
     [c-sidebar]]))


(defn c-listing
  []
  (let [active-page (re/subscribe [::router.subs/active-page])]
    (fn []
      (let [{query :query} @active-page
            *current-sidebar-choice (or (keyword (:sidebar query)) :my-employer-job-listing)]
        [:div.listing
         (case *current-sidebar-choice
           ;; Employer
           :my-employer-job-listing [c-my-employer-job-listing]
           :my-employer-contract-listing [c-my-employer-contract-listing]
           :my-employer-invoice-listing [c-my-employer-invoice-listing]
           :my-employer-dispute-listing [c-my-employer-dispute-listing]

           ;; Candidate
           :my-candidate-contract-listing [c-my-candidate-contract-listing]
           :my-candidate-invoice-listing [c-my-candidate-invoice-listing]
           :my-candidate-dispute-listing [c-my-candidate-dispute-listing]

           ;; Arbiter
           :my-arbiter-job-listing [c-my-arbiter-job-listing]
           :my-arbiter-dispute-listing [c-my-arbiter-dispute-listing]

           (throw (ex-info "Unable to determine sidebar choice" {:current-sidebar-choice *current-sidebar-choice})))]))))


(defmethod page :route.me/index []
  (fn []
    [c-main-layout {:container-opts {:class :my-contracts-main-container}}
     [c-sidebar]
     [c-mobile-navigation]
     [c-listing]]))
