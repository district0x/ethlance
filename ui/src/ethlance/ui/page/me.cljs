(ns ethlance.ui.page.me
  (:require [district.ui.component.page :refer [page]]
            [district.ui.router.subs :as router.subs]
            [district.ui.router.events :as router.events]
            [ethlance.ui.component.circle-button :refer [c-circle-icon-button]]
            [ethlance.ui.component.pagination :refer [c-pagination-ends]]
            [ethlance.ui.component.main-layout :refer [c-main-layout]]
            [ethlance.ui.component.mobile-sidebar :refer [c-mobile-sidebar]]
            [ethlance.ui.component.table :refer [c-table]]
            [ethlance.ui.component.tabular-layout :refer [c-tabular-layout]]
            [ethlance.ui.component.button :refer [c-button c-button-label]]
            [ethlance.ui.util.navigation :as util.navigation]
            [ethlance.ui.util.dates :refer [relative-ago formatted-date]]
            [district.ui.graphql.subs :as gql]
            [ethlance.ui.util.tokens :as tokens]
            [re-frame.core :as re]))

(defn c-nav-sidebar-element [label id-value]
  (let [*active-page (re/subscribe [::router.subs/active-page])]
    (fn []
      (let [{active-page :name
             active-params :param
             active-query :query} @*active-page
            updated-query (-> (or active-query {})
                              (assoc  :sidebar id-value)
                              (dissoc :tab))
            *current-sidebar-choice (keyword (:sidebar active-query))]
        [:div.nav-element
         [:a.link
          {:title (str "Navigate to '" label "'")
           :class [(when (= *current-sidebar-choice id-value) "active")]
           :href (util.navigation/resolve-route {:route active-page :params active-params :query updated-query})
           :on-click (util.navigation/create-handler {:route active-page :params active-params :query updated-query})}
          label]]))))

(defn link-params [{:keys [route params]}]
  {:on-click (util.navigation/create-handler {:route route
                                              :params params})
   :href (util.navigation/resolve-route {:route route
                                         :params params})})
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
     (into [c-table {:headers (map :title headers)}]
           (mapv (fn [row]
                  (mapv (fn [header]
                         (if (nil? link-params-fn)
                           [:span ((:source header) row)]
                           [:a (link-params (link-params-fn row)) [:span ((:source header) row)]]))
                       headers))
                 rows))
     [c-pagination-ends
      {:total-count total-count
       :limit limit
       :offset offset
       :set-offset-event :page.me/set-pagination-offset}]]))

(defn tab-navigate-handler [sidebar tab]
  (fn []
    (re/dispatch [::router.events/navigate
                  :route.me/index
                  {}
                  {:sidebar sidebar :tab tab}])))

(defn c-job-listing [user-type]
  (let [active-user (:user/id @(re/subscribe [:ethlance.ui.subscriptions/active-session]))
        url-query @(re/subscribe [::router.subs/active-page-query])
        tab (or (:tab url-query) "active")
        tab-to-index {"active" 0 "finished" 1}
        tab-index (get tab-to-index tab 0)
        limit @(re/subscribe [:page.me/pagination-limit])
        offset @(re/subscribe [:page.me/pagination-offset])
        job-query [:job-search {:search-params {user-type active-user :status tab} :limit limit :offset offset}
                   [:total-count
                    [:items [:job/id
                             :job/title
                             :job/status
                             :job/token-amount
                             :job/token-type
                             :job/date-created
                             [:token-details [:token-detail/id
                                              :token-detail/name
                                              :token-detail/symbol]]]]]]
        result @(re/subscribe [::gql/query {:queries [job-query]}])
        jobs (get-in result [:job-search :items])
        remuneration (fn [job] (str (tokens/human-amount (:job/token-amount job) (:job/token-type job))
                                    " " (-> job :token-details :token-detail/symbol)))
        jobs-table [{:title "Job Title" :source :job/title}
                    {:title "Remuneration" :source remuneration}
                    {:title "Created at" :source (partial formatted-date :job/date-created)}]
        job-link-fn (fn [job] {:route :route.job/detail :params {:id (:job/id job)}})
        pagination {:total-count (get-in result [:job-search :total-count]) ; FIXME: not correct, as the jobs are queried once but filtered in UI
                    :limit limit
                    :offset offset}
        user->section {:employer :my-employer-job-listing
                       :creator :my-employer-job-listing
                       :arbiter :my-arbiter-job-listing}]
  [c-tabular-layout
   {:key "my-employer-job-tab-listing"
    :default-tab tab-index}

   {:label "Active Jobs" :on-click (tab-navigate-handler (user->section user-type) :active)}
   [:div.listing.my-employer-job-listing
    [c-table-listing jobs-table jobs job-link-fn pagination]]

   {:label "Finished Jobs" :on-click (tab-navigate-handler (user->section user-type) :finished)}
   [:div.listing.my-employer-job-listing
    [c-table-listing jobs-table jobs job-link-fn pagination]]]))

(defn c-my-employer-job-listing []
  [c-job-listing :creator])

(defn c-contract-listing [query results-getter]
  (let [jobs @(re/subscribe [::gql/query {:queries [query]}])
        filter-by-status (fn [jobs status] (filter #(= status (:job-story/status %)) (results-getter jobs)))
        user-name-fn (fn [job] (get-in job [:candidate :user :user/name]))
        jobs-table [{:title "Job Title" :source #(get-in % [:job :job/title])}
                    {:title "Candidate" :source user-name-fn}
                    {:title "Created at" :source (partial formatted-date :job-story/date-created)}]
        contract-link-fn (fn [job] {:route :route.job/contract :params {:job-story-id (:job-story/id job)}})]
  [c-tabular-layout
   {:key "my-employer-job-tab-listing"
    :default-tab 0}

   {:label "Invitations"}
   [:div.listing.my-employer-job-listing
    [c-table-listing jobs-table (filter-by-status jobs :invitation) contract-link-fn]]

   {:label "Pending Proposals"}
   [:div.listing.my-employer-job-listing
    [c-table-listing jobs-table (filter-by-status jobs :proposal) contract-link-fn]]

   {:label "Active Contracts"}
   [:div.listing.my-employer-job-listing
    [c-table-listing jobs-table (filter-by-status jobs :active) contract-link-fn]]

   {:label "Finished Contracts"}
   [:div.listing.my-employer-job-listing
    [c-table-listing jobs-table (filter-by-status jobs :finished) contract-link-fn]]]))

(defn c-invoice-listing [query-params]
  (let [query [:invoice-search query-params
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
                        :token-detail/symbol]]]]]]
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
        invoices (get-in result [:invoice-search :items])
        filter-by-status (fn [invoices status] (filter #(= status (:invoice/status %)) invoices))
        user-name-fn (fn [invoice] (get-in invoice [:creation-message :creator :user/name]))
        invoice-date-created-fn (partial formatted-date #(get-in % [:creation-message :message/date-created]))
        amount-requested-fn (fn [invoice]
                              (str (tokens/human-amount (:invoice/amount-requested invoice)
                                                        (get-in invoice [:job-story :job :job/token-type]))
                                    " " (get-in invoice [:job-story :job :token-details :token-detail/symbol])))
        table [{:title "Job Title" :source #(get-in % [:job-story :job :job/title])}
               {:title "Candidate" :source user-name-fn}
               {:title "Amount Requested" :source amount-requested-fn}
               {:title "Created at" :source invoice-date-created-fn}]
        invoice-link-fn (fn [invoice]
                          {:route :route.invoice/index
                           :params {:job-id (:job/id invoice) :invoice-id (:invoice/id invoice)}})]
  [c-tabular-layout
   {:key "my-employer-job-tab-listing"
    :default-tab 0}

   {:label "Pending Invoices"}
   [:div.listing.my-employer-job-listing
    [c-table-listing table (filter-by-status invoices "created") invoice-link-fn]]

   {:label "Paid Invoices"}
   [:div.listing.my-employer-job-listing
    [c-table-listing table (filter-by-status invoices "paid") invoice-link-fn]]]))

(defn c-dispute-listing [user-type]
  (let [active-user (:user/id @(re/subscribe [:ethlance.ui.subscriptions/active-session]))
        ; Hard-coded user addresses for testing to avoid account switching
        users {:candidate "0x6dFA8c7DD1658387D170a2F9F34452C6f873fB9e"
               :employer "0x0935D2ec65144343df67Bd3c7399c37Beb1bBce0"
               :arbiter "0x2edd65Db76A4c02E851702CAc5E51b77Dc721cf0"}
        query [:dispute-search {user-type (get users user-type)}
               [:total-count
                [:items [:id
                         :invoice/id
                         :dispute/reason
                         :dispute/resolution
                         :dispute/date-created
                         :dispute/date-resolved

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
                             :token-detail/symbol]]]]]]]]
        disputes-result @(re/subscribe [::gql/query {:queries [query]}])
        disputes (get-in  disputes-result [:dispute-search :items])
        filter-by-status (fn [invoices status] (filter #(= status (:invoice/status %)) invoices))
        candidate-name-fn (fn [invoice] (get-in invoice [:candidate :user :user/name]))
        arbiter-name-fn (fn [invoice] (get-in invoice [:arbiter :user :user/name]))
        arbiter-name-fn (fn [invoice] (get-in invoice [:dispute-resolved-message :creator :user/name]))
        dispute-date-created-fn (partial formatted-date #(get-in % [:dispute/date-created]))
        dispute-date-resolved-fn (partial formatted-date #(get-in % [:dispute/date-resolved]))
        amount-fn (fn [amount-source invoice]
                              (str (tokens/human-amount (amount-source invoice) (get-in invoice [:job :job/token-type]))
                                    " " (get-in invoice [:job :token-details :token-detail/symbol])))
        truncated-dispute-fn (fn [text-source invoice]
                               (let [text (get-in invoice [text-source])
                                     max-chars 20]
                                 (str (subs text 0 (min (count text) max-chars)) "...")))
        open-table [{:title "Job Title" :source #(get-in % [:job :job/title])}
                    {:title "Candidate" :source candidate-name-fn}
                    {:title "Requested amount" :source (partial amount-fn :invoice/amount-requested)}
                    {:title "Reason" :source (partial truncated-dispute-fn :dispute/reason)}
                    {:title "Date raised" :source dispute-date-created-fn}]
        resolved-table [{:title "Job Title" :source #(get-in % [:job :job/title])}
                        {:title "Arbiter" :source arbiter-name-fn}
                        {:title "Resolution" :source (partial truncated-dispute-fn :dispute/resolution)}
                        {:title "Received amount" :source (partial amount-fn :invoice/amount-paid)}
                        {:title "Date resolved" :source dispute-date-resolved-fn}]]
  [c-tabular-layout
   {:key "my-employer-job-tab-listing"
    :default-tab 0}

   {:label "Open Disputes"}
   [:div.listing.my-employer-job-listing
    [c-table-listing open-table (filter #(nil? (:dispute/date-resolved %)) disputes)]]

   {:label "Resolved Disputes"}
   [:div.listing.my-employer-job-listing
    [c-table-listing resolved-table (filter #(not (nil? (:dispute/date-resolved %))) disputes)]]]))

(defn c-my-employer-contract-listing []
  (let [active-user (:user/id @(re/subscribe [:ethlance.ui.subscriptions/active-session]))
        results-getter (fn [results] (get-in results [:employer :job-stories :items]))
        query [:employer {:user/id active-user}
               [:user/id
                [:job-stories
                 [:total-count
                  [:items [:job/id
                           :job-story/id
                           :job-story/status
                           :job-story/date-created
                           :job-story/proposal-rate
                           [:candidate
                            [:user/id
                             [:user [:user/name]]]]
                           [:job [:job/title]]]]]]]]]
    [c-contract-listing query results-getter]))

(defn c-my-employer-invoice-listing []
  (let [employer (:user/id @(re/subscribe [:ethlance.ui.subscriptions/active-session]))]
    [c-invoice-listing {:employer employer}]))

(defn c-my-employer-dispute-listing []
  (c-dispute-listing :employer))

;;
;; Candidate Sections
;;

(defn c-my-candidate-contract-listing []
  (let [active-user (:user/id @(re/subscribe [:ethlance.ui.subscriptions/active-session]))
        results-getter (fn [results] (get-in results [:candidate :job-stories :items]))
        query [:candidate {:user/id active-user}
               [:user/id
                [:job-stories
                 [:total-count
                  [:items [:job/id
                           :job-story/id
                           :job-story/status
                           :job-story/date-created
                           :job-story/proposal-rate
                           [:candidate
                            [:user/id
                             [:user [:user/name]]]]
                           [:job [:job/title]]]]]]]]]
    [c-contract-listing query results-getter]))

(defn c-my-candidate-invoice-listing []
  (let [candidate (:user/id @(re/subscribe [:ethlance.ui.subscriptions/active-session]))]
    [c-invoice-listing {:candidate candidate}]))

(defn c-my-candidate-dispute-listing []
  [c-dispute-listing :candidate])

;;
;; Arbiter Sections
;;

(defn c-my-arbiter-job-listing []
  [c-job-listing :arbiter])

(defn c-my-arbiter-dispute-listing []
  [c-dispute-listing :arbiter])

(defn c-sidebar
  []
  (fn []
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
      [c-nav-sidebar-element "My Disputes" :my-arbiter-dispute-listing]]]))

(defn c-mobile-navigation
  []
  (fn []
    [c-mobile-sidebar
     [c-sidebar]]))

(defn c-listing []
  (let [active-page (re/subscribe [::router.subs/active-page])]
    (fn []
      (let [{page :name
             params :param
             query :query} @active-page
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

           (throw (ex-info "Unable to determine sidebar choice" *current-sidebar-choice)))]))))

(defmethod page :route.me/index []
  (fn []
    [c-main-layout {:container-opts {:class :my-contracts-main-container}}
     [c-sidebar]
     [c-mobile-navigation]
     [c-listing]]))
