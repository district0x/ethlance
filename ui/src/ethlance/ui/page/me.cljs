(ns ethlance.ui.page.me
  (:require [district.ui.component.page :refer [page]]
            [district.ui.router.subs :as router.subs]
            [ethlance.ui.component.circle-button :refer [c-circle-icon-button]]
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
                              (dissoc :section))
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
  [headers rows & [link-params-fn]]
  [:<>
   (into [c-table {:headers (map :title headers)}]
         (mapv (fn [row]
                (mapv (fn [header]
                       (if (nil? link-params-fn)
                         [:span ((:source header) row)]
                         [:a (link-params (link-params-fn row)) [:span ((:source header) row)]]))
                     headers))
               rows))
   [:div.button-listing
    [c-circle-icon-button {:name :ic-arrow-left2 :size :smaller :disabled? true}]
    [c-circle-icon-button {:name :ic-arrow-left :size :smaller :disabled? true}]
    [c-circle-icon-button {:name :ic-arrow-right :size :smaller :disabled? true}]
    [c-circle-icon-button {:name :ic-arrow-right2 :size :smaller :disabled? true}]]])


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
                    {:title "Created at" :source (partial formatted-date :job/date-created)}]
        job-link-fn (fn [job] {:route :route.job/detail :params {:id (:job/id job)}})]
  [c-tabular-layout
   {:key "my-employer-job-tab-listing"
    :default-tab 0}

   {:label "Active Jobs"}
   [:div.listing.my-employer-job-listing
    [c-table-listing jobs-table active-jobs job-link-fn]]

   {:label "Finished Jobs"}
   [:div.listing.my-employer-job-listing
    [c-table-listing jobs-table finished-jobs job-link-fn]]]))

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

(defn c-my-employer-invoice-listing []
  (let [active-user (:user/id @(re/subscribe [:ethlance.ui.subscriptions/active-session]))
        query [:job-search {:search-params {:creator active-user}}
                   [:total-count
                    [:items [:job/id
                             :job/title
                             :job/token-type
                             [:invoices
                              [:total-count
                               [:items [:id
                                        :job/id
                                        :invoice/status
                                        :invoice/id
                                        :invoice/amount-requested
                                        :invoice/date-requested
                                        :invoice/date-paid
                                        :invoice/amount-paid
                                        [:creation-message
                                         [:message/id
                                          :message/date-created
                                          [:creator [:user/name]]]]]]]]
                             [:token-details [:token-detail/id
                                              :token-detail/name
                                              :token-detail/symbol]]]]]]
        jobs-result @(re/subscribe [::gql/query {:queries [query]}])
        jobs-with-invoices (filter #(> (get-in % [:invoices :total-count]) 0) (get-in jobs-result [:job-search :items]))
        invoices (reduce (fn [invoices job]
                           (reduce (fn [invoices invoice]
                                     (conj invoices (merge invoice (select-keys job [:job/title :job/token-type :token-details]))))
                                   invoices (get-in job [:invoices :items])))
                         [] jobs-with-invoices)
        filter-by-status (fn [invoices status] (filter #(= status (:invoice/status %)) invoices))
        user-name-fn (fn [invoice] (get-in invoice [:creation-message :creator :user/name]))
        invoice-date-created-fn (partial formatted-date #(get-in % [:creation-message :message/date-created]))
        amount-requested-fn (fn [invoice]
                              (str (tokens/human-amount (:invoice/amount-requested invoice) (:job/token-type invoice))
                                    " " (-> invoice :token-details :token-detail/symbol)))
        table [{:title "Job Title" :source #(get-in % [:job/title])}
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

(defn c-my-employer-dispute-listing []
  (let [active-user (:user/id @(re/subscribe [:ethlance.ui.subscriptions/active-session]))
        query [:job-search {:search-params {:creator active-user}}
                   [:total-count
                    [:items [:job/id
                             :job/title
                             :job/token-type
                             [:invoices
                              [:total-count
                               [:items [:id
                                        :job/id
                                        :job-story/id
                                        :invoice/status
                                        :invoice/id
                                        :invoice/amount-requested
                                        :invoice/date-requested
                                        :invoice/date-paid
                                        :invoice/amount-paid
                                        [:dispute-raised-message [:message/id
                                                                  :message/text
                                                                  [:creator [:user/name]]]]
                                        [:dispute-resolved-message [:message/id
                                                                    :message/text
                                                                    [:creator [:user/name]]]]
                                        ]]]]
                             [:token-details [:token-detail/id
                                              :token-detail/name
                                              :token-detail/symbol]]]]]]
        jobs-result @(re/subscribe [::gql/query {:queries [query]}])
        jobs-with-invoices (filter #(> (get-in % [:invoices :total-count]) 0) (get-in jobs-result [:job-search :items]))
        invoices (reduce (fn [invoices job]
                           (reduce (fn [invoices invoice]
                                     (if (not (nil? (get-in invoice [:dispute-raised-message :message/id])))
                                       (conj invoices (merge invoice (select-keys job [:job/title :job/token-type :token-details])))
                                       invoices))
                                   invoices (get-in job [:invoices :items])))
                         [] jobs-with-invoices)
        filter-by-status (fn [invoices status] (filter #(= status (:invoice/status %)) invoices))
        candidate-name-fn (fn [invoice] (get-in invoice [:dispute-raised-message :creator :user/name]))
        arbiter-name-fn (fn [invoice] (get-in invoice [:dispute-resolved-message :creator :user/name]))
        invoice-date-created-fn (partial formatted-date #(get-in % [:creation-message :message/date-created]))
        amount-fn (fn [amount-source invoice]
                              (str (tokens/human-amount (amount-source invoice) (:job/token-type invoice))
                                    " " (-> invoice :token-details :token-detail/symbol)))
        truncated-dispute-fn (fn [text-source invoice]
                               (let [text (get-in invoice [text-source :message/text])
                                     max-chars 20]
                                 (str (subs text 0 (min (count text) max-chars)) "...")))
        open-table [{:title "Job Title" :source #(get-in % [:job/title])}
                    {:title "Candidate" :source candidate-name-fn}
                    {:title "Requested amount" :source (partial amount-fn :invoice/amount-requested)}
                    {:title "Dispute text" :source (partial truncated-dispute-fn :dispute-raised-message)}
                    {:title "Date raised" :source invoice-date-created-fn}]
        resolved-table [{:title "Job Title" :source #(get-in % [:job/title])}
                        {:title "Arbiter" :source arbiter-name-fn}
                        {:title "Resolution text" :source (partial truncated-dispute-fn :dispute-resolved-message)}
                        {:title "Received amount" :source (partial amount-fn :invoice/amount-paid)}
                        {:title "Date resolved" :source invoice-date-created-fn}] ]
  [c-tabular-layout
   {:key "my-employer-job-tab-listing"
    :default-tab 0}

   {:label "Open Disputes"}
   [:div.listing.my-employer-job-listing
    [c-table-listing open-table (filter-by-status invoices "created")]]

   {:label "Resolved Disputes"}
   [:div.listing.my-employer-job-listing
    [c-table-listing resolved-table (filter-by-status invoices "paid")]]]))

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
        ; *current-sidebar-choice (re/subscribe [:page.me/current-sidebar-choice])
        active-page (re/subscribe [::router.subs/active-page])
        ]

    (fn []
      (let [{page :name
             params :param
             query :query} @active-page
            *current-sidebar-choice (or (keyword (:sidebar query)) :my-employer-job-listing)
            ]
        (println ">>> c-listing" @active-page)
        [:div.listing
         (case *current-sidebar-choice
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

           (throw (ex-info "Unable to determine sidebar choice" *current-sidebar-choice)))]))))

(defmethod page :route.me/index []
  (fn []
    [c-main-layout {:container-opts {:class :my-contracts-main-container}}
     [c-sidebar]
     [c-mobile-navigation]
     [c-listing]]))
