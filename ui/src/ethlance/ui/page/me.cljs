(ns ethlance.ui.page.me
  (:require [district.ui.component.page :refer [page]]
            [district.format :as format]
            [cljs-time.core :as t-core]
            [cljs-time.coerce :as t-coerce]
            [district.ui.router.subs :as router.subs]
            [ethlance.ui.component.circle-button :refer [c-circle-icon-button]]
            [ethlance.ui.component.main-layout :refer [c-main-layout]]
            [ethlance.ui.component.mobile-sidebar :refer [c-mobile-sidebar]]
            [ethlance.ui.component.table :refer [c-table]]
            [ethlance.ui.component.tabular-layout :refer [c-tabular-layout]]
            [ethlance.ui.component.button :refer [c-button c-button-label]]
            [ethlance.ui.util.navigation :as util.navigation]
            [district.ui.graphql.subs :as gql]
            [ethlance.ui.util.tokens :as tokens]
            [re-frame.core :as re]))

(defn c-nav-sidebar-element [label location]
  (let [*current-sidebar-choice (re/subscribe [:page.me/current-sidebar-choice])
        *active-page (re/subscribe [::router.subs/active-page])]
    (fn []
      (let [{active-page :name
             active-params :param
             active-query :query} @*active-page
            updated-query (assoc (or active-query {}) :sidebar (name location))]
        [:div.nav-element
         [:a.link
          {:title (str "Navigate to '" label "'")
           :class [(when (= @*current-sidebar-choice location) "active")]
           :href (util.navigation/resolve-route {:route active-page :params active-params :query updated-query})
           :on-click (fn [e]
                       (re/dispatch [:page.me/change-sidebar-choice location])
                       (.preventDefault e)
                       nil)}
          label]]))))

(defn c-default-listing []
  [:<>
   [c-table
    {:headers ["Title" "Candidate" "Rate" "Total Spent" ""]}
    [[:span "Cryptoeconomics Research Intern"]
     [:span "Keegan Quigley"]
     [:span "$30/hr"]
     [:span "12.2 ETH"]]

    [[:span "Smart Contract Hacker"]
     [:span "Cyrus Karsen"]
     [:span "$25"]
     [:span "1000 SNT"]
     ]

    [[:span "Interactive Developer"]
     [:span "Ari Kaplan"]
     [:span "$75"]
     [:span "5.4 ETH"]
     ]

    [[:span "Cryptoeconomics Research Intern"]
     [:span "Keegan Quigley"]
     [:span "$30/hr"]
     [:span "12.2 ETH"]
     ]]
   [:div.button-listing
    [c-circle-icon-button {:name :ic-arrow-left2 :size :smaller :disabled? true}]
    [c-circle-icon-button {:name :ic-arrow-left :size :smaller :disabled? true}]
    [c-circle-icon-button {:name :ic-arrow-right :size :smaller :disabled? true}]
    [c-circle-icon-button {:name :ic-arrow-right2 :size :smaller :disabled? true}]]])

(defn relative-ago [get-date-field data]
  (format/time-ago (t-core/minus (t-core/now) (t-coerce/from-long (get-date-field data)))))

(defn formatted-date [get-date-field data]
  (format/format-date (t-coerce/from-long (get-date-field data))))

(defn c-table-listing
  "Produces tabl ewith headers

  Arguments:
    headers [{:title \"Column title\" :source <callable to get from rows seq map>}]
    rows    seq of maps

  Example:
    (c-table-listing [{:title \"Name\" :source :user/name}] [{:user/name \"John Doe\"}])"
  [headers rows]
  [:<>
   (into [c-table {:headers (map :title headers)}]
         (map (fn [row]
                (map (fn [header] [:span ((:source header) row)]) headers))
               rows))
   [:div.button-listing
    [c-circle-icon-button {:name :ic-arrow-left2 :size :smaller :disabled? true}]
    [c-circle-icon-button {:name :ic-arrow-left :size :smaller :disabled? true}]
    [c-circle-icon-button {:name :ic-arrow-right :size :smaller :disabled? true}]
    [c-circle-icon-button {:name :ic-arrow-right2 :size :smaller :disabled? true}]]])

(defn c-my-employer-job-listing-example []
  [c-tabular-layout
   {:key "my-employer-job-tab-listing"
    :default-tab 0}

   {:label "Invitations"}
   [:div.listing.my-employer-job-listing
    [c-default-listing]]

   {:label "Pending Proposals"}
   [:div.listing
    [c-default-listing]]

   {:label "Active Contracts"}
   [:div.listing
    [c-default-listing]]

   {:label "Finished Contracts"}
   [:div.listing
    [c-default-listing]]

   {:label "Canceled Contracts"}
   [:div.listing
    [c-default-listing]]])

(defn c-my-employer-job-listing []
  (let [active-user (:user/id @(re/subscribe [:ethlance.ui.subscriptions/active-session]))
        job-query [:job-search {:search-params {:creator active-user}}
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
        jobs @(re/subscribe [::gql/query {:queries [job-query]}])
        active-jobs (filter #(= :active (:job/status %)) (get-in jobs [:job-search :items]))
        finished-jobs (filter #(= :finished (:job/status %)) (get-in jobs [:job-search :items]))
        remuneration (fn [job] (str (tokens/human-amount (:job/token-amount job) (:job/token-type job))
                                    " " (-> job :token-details :token-detail/symbol)))
        jobs-table [{:title "Job Title" :source :job/title}
                    {:title "Remuneration" :source remuneration}
                    {:title "Created at" :source (partial formatted-date :job/date-created)}]]
  [c-tabular-layout
   {:key "my-employer-job-tab-listing"
    :default-tab 0}

   {:label "Active Jobs"}
   [:div.listing.my-employer-job-listing
    [c-table-listing jobs-table active-jobs]]

   {:label "Finished Jobs"}
   [:div.listing.my-employer-job-listing
    [c-table-listing jobs-table finished-jobs]]]))

(defn c-my-employer-contract-listing []
  (let [active-user (:user/id @(re/subscribe [:ethlance.ui.subscriptions/active-session]))
        query [:employer {:user/id active-user}
               [:user/id
                [:employer/job-stories
                 [:total-count
                  [:items [:job/id
                           :job-story/id
                           :job-story/status
                           :job-story/date-created
                           :job-story/proposal-rate
                           [:candidate
                            [:user/id
                             [:user [:user/name]]]]
                           [:job [:job/title]]]]]]]]
        jobs @(re/subscribe [::gql/query {:queries [query]}])
        filter-by-status (fn [jobs status] (filter #(= status (:job-story/status %)) (get-in jobs [:employer :employer/job-stories :items])))
        user-name-fn (fn [job] (get-in job [:candidate :user :user/name]))
        jobs-table [{:title "Job Title" :source #(get-in % [:job :job/title])}
                    {:title "Candidate" :source user-name-fn}
                    {:title "Created at" :source (partial formatted-date :job-story/date-created)}]]
  [c-tabular-layout
   {:key "my-employer-job-tab-listing"
    :default-tab 0}

   {:label "Invitations"}
   [:div.listing.my-employer-job-listing
    [c-table-listing jobs-table (filter-by-status jobs :invitation)]]

   {:label "Pending Proposals"}
   [:div.listing.my-employer-job-listing
    [c-table-listing jobs-table (filter-by-status jobs :proposal)]]

   {:label "Active Contracts"}
   [:div.listing.my-employer-job-listing
    [c-table-listing jobs-table (filter-by-status jobs :active)]]

   {:label "Finished Contracts"}
   [:div.listing.my-employer-job-listing
    [c-table-listing jobs-table (filter-by-status jobs :finished)]]]))

(defn c-my-employer-invoice-listing []
  (let [active-user (:user/id @(re/subscribe [:ethlance.ui.subscriptions/active-session]))
        query [:job-search {:search-params {:creator active-user}}
                   [:total-count
                    [:items [:job/id
                             :job/title
                             [:invoices
                              [:total-count
                               [:items [:id
                                        :job/id
                                        :invoice/status
                                        :invoice/id
                                        :invoice/date-paid
                                        :invoice/amount-requested
                                        :invoice/amount-paid
                                        [:creation-message
                                         [:message/id
                                          [:creator [:user/name]]]]]]]]
                             [:token-details [:token-detail/id
                                              :token-detail/name
                                              :token-detail/symbol]]]]]]
        jobs-result @(re/subscribe [::gql/query {:queries [query]}])
        jobs (get-in jobs-result [:employer :employer/job-stories :items])
        filter-by-status (fn [jobs status] (filter #(=  (:job-story/status %)) jobs))
        pending-invoices (reduce (fn [invoices job]
                                   (reduce (fn [invoices invoice]
                                             (if (nil? (:invoice/date-paid invoice))
                                               (into invoices (assoc invoice :job-title (:job/title job)))
                                               invoices))
                                           invoices (get-in job [:invoices :items])))
                                 [] jobs)
        user-name-fn (fn [invoice] (get-in invoice [:creation-message :creator :user/name]))
        invoice-date-created-fn (partial formatted-date #(get-in % [:creation-message :message/date-created]))
        jobs-table [{:title "Job Title" :source #(get-in % [:job-title])}
                    {:title "Candidate" :source user-name-fn}
                    {:title "Created at" :source invoice-date-created-fn}]]
  [c-tabular-layout
   {:key "my-employer-job-tab-listing"
    :default-tab 0}

   {:label "Pending Invoices"}
   [:div.listing.my-employer-job-listing
    [c-table-listing jobs-table pending-invoices]]

   {:label "Paid Invoices"}
   [:div.listing.my-employer-job-listing
    [c-table-listing jobs-table pending-invoices]]]))

(defn c-my-employer-dispute-listing []
  [:div.not-implemented "Not Implemented - Employer - My Disputes"])

;;
;; Candidate Sections
;;

(defn c-my-candidate-job-listing []
  [:div.not-implemented "Not Implemented - Candidate - My Jobs"])

(defn c-my-candidate-contract-listing []
  [:div.not-implemented "Not Implemented - Candidate - My Contracts"])

(defn c-my-candidate-invoice-listing []
  [:div.not-implemented "Not Implemented - Candidate - My Invoices"])

(defn c-my-candidate-dispute-listing []
  [:div.not-implemented "Not Implemented - Candidate - My Disputes"])

;;
;; Arbiter Sections
;;

(defn c-my-arbiter-job-listing []
  [:div.not-implemented "Not Implemented - Arbiter - My Jobs"])

(defn c-my-arbiter-contract-listing []
  [:div.not-implemented "Not Implemented - Arbiter - My Contracts"])

(defn c-my-arbiter-dispute-listing []
  [:div.not-implemented "Not Implemented - Arbiter - My Disputes"])

(defn c-sidebar
  []
  (fn []
    [:div.sidebar
     [:div.section
      [:div.label "Employer"]
      [c-nav-sidebar-element "My Jobs" :my-employer-job-listing]
      [c-nav-sidebar-element "My Contracts" :my-employer-contract-listing]
      [c-nav-sidebar-element "My Invoices" :my-employer-invoice-listing]
      [c-nav-sidebar-element "My Disputes" :my-employer-dispute-listing]]

     [:div.section
      [:div.label "Candidate"]
      [c-nav-sidebar-element "My Jobs" :my-candidate-job-listing]
      [c-nav-sidebar-element "My Contracts" :my-candidate-contract-listing]
      [c-nav-sidebar-element "My Invoices" :my-candidate-invoice-listing]
      [c-nav-sidebar-element "My Disputes" :my-candidate-dispute-listing]]

     [:div.section
      [:div.label "Arbiter"]
      [c-nav-sidebar-element "My Jobs" :my-arbiter-job-listing]
      [c-nav-sidebar-element "My Contracts" :my-arbiter-contract-listing]
      [c-nav-sidebar-element "My Invoices" :my-arbiter-invoice-listing]
      [c-nav-sidebar-element "My Disputes" :my-arbiter-dispute-listing]]]))

(defn c-mobile-navigation
  []
  (fn []
    [c-mobile-sidebar
     [c-sidebar]]))

(defn c-listing []
  (let [
        *current-sidebar-choice (re/subscribe [:page.me/current-sidebar-choice])
        ]
    (fn []
      [:div.listing
       (case @*current-sidebar-choice
         ;; Employer
         :my-employer-job-listing [c-my-employer-job-listing]
         :my-employer-contract-listing [c-my-employer-contract-listing]
         :my-employer-invoice-listing [c-my-employer-invoice-listing]
         :my-employer-dispute-listing [c-my-employer-dispute-listing]

         ;; Candidate
         :my-candidate-job-listing [c-my-candidate-job-listing]
         :my-candidate-contract-listing [c-my-candidate-contract-listing]
         :my-candidate-invoice-listing [c-my-candidate-invoice-listing]
         :my-candidate-dispute-listing [c-my-candidate-dispute-listing]

         ;; Arbiter
         :my-arbiter-job-listing [c-my-arbiter-job-listing]
         :my-arbiter-contract-listing [c-my-arbiter-contract-listing]
         :my-arbiter-dispute-listing [c-my-arbiter-dispute-listing]

         (throw (ex-info "Unable to determine sidebar choice" @*current-sidebar-choice)))])))

(defmethod page :route.me/index []
  (fn []
    [c-main-layout {:container-opts {:class :my-contracts-main-container}}
     [c-sidebar]
     [c-mobile-navigation]
     [c-listing]]))
