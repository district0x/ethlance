(ns ethlance.server.db
  "Represents the ethlance in-memory sqlite database. Contains a mount
  component for creating the in-memory database upon initial load."
  (:require
    [cljs.core.async :as async :refer [go-loop <! take!]]
    [clojure.pprint :as pprint]
    [clojure.set :as set]
    [clojure.walk]
    [com.rpl.specter :as $ :include-macros true]
    [cuerdas.core :as str]
    [district.server.async-db :as db]
    [district.server.config :refer [config]]
    [ethlance.server.db.schema :refer [database-schema]]
    [district.shared.async-helpers :refer [<? safe-go]]
    [honeysql.core :as sql]
    [mount.core :as mount :refer [defstate]]
    [taoensso.timbre :as log]))


(declare start stop)
(defonce db-state (atom nil))


(def mount-state-key
  "Key defining our mount component within the district configuration"
  :ethlance/db)


(defstate ^{:on-reload :noop} ethlance-db
  :start (start (merge (get @config mount-state-key)
                       (mount-state-key (mount/args))))
  :stop (stop))


(defn print-db
  "(print-db) prints all db tables to the repl
   (print-db :users) prints only users table"
  ([_] (print-db nil))
  ([conn table]
   (safe-go
     (let [select (fn [& [select-fields & r]]
                    (pprint/print-table (<? (db/all conn (->> (partition 2 r)
                                                              (map vec)
                                                              (into {:select select-fields}))))))
           all-tables (if table
                        [(name table)]
                        (->> (<? (db/all conn {:select [:name] :from [:sqlite-master] :where [:= :type "table"]}))
                             (map :name)))]
       (doseq [t all-tables]
         (println "#######" (str/upper t) "#######")
         (select [:*] :from [(keyword t)]))))))


#_(defn table-exists?
  [name]
  (contains? (set (list-tables)) name))


(defn- get-table-schema
  "Retrieve the given table schema defined by `table-name` from the
  database schema, or nil."
  [table-name]
  (let [schema (-> ($/select [$/ALL #(= (:table-name %) table-name)] database-schema)
                   first)
        id-keys (->> schema
                     :table-columns
                     (some (fn [[{:keys [name args]}]] (when (= name :primary-key) args))))]
    (assoc schema :id-keys id-keys)))


#_(defn- get-table-pk-columns [table-name]
  (let [table-schema (get-table-schema table-name)]
    (->> (:table-columns table-schema)
         (mapcat (fn [x]
                   (when-not (keyword? (first x))
                     (let [{:keys [name args]} (first x)]
                       (when (= name :primary-key)
                         args))))))))


(defn- get-table-columns-by-type
  [table-name type-pred]
  (let [table-schema (get-table-schema table-name)]
    (->> (:table-columns table-schema)
         (filter (comp keyword? first))
         (filter (comp type-pred second)))))


(defn- get-table-column-names
  "Retrieves the table column names for a given table schema defined by their `table-name`."
  [table-name]
  (let [table-schema (get-table-schema table-name)]
    (->> (:table-columns table-schema)
         (map first)
         (filter keyword?))))


(defn filter-tables
  [table-names schema]
  (filter #((set table-names) (:table-name %)) schema))


(defn create-db!
  "Creates the database with tables defined in the `database-schema`."
  [conn]
  (safe-go
    (log/info "Creating Sqlite Database...")
    (doseq [{:keys [table-name table-columns]} database-schema]
      (<? (db/run! conn {:create-table [table-name :if-not-exists] :with-columns [table-columns]})))))


(defn drop-db!
  "Drops all of the database tables defined in the `database-schema`."
  [conn]
  (safe-go
    (log/info "Dropping Sqlite Database...")
    (doseq [{:keys [table-name]} (reverse database-schema)]
      (log/debug (str/format "Dropping Database Table '%s' ..." table-name) {:conn conn})
      (<? (db/run! conn {:drop-table [:if-exists table-name]}))
      #_(log/debug "DONE"))))


;;
;; Row manipulation utils ;;
;;

(def auto-increment-types #{:serial})


(defn insert-row!
  "Inserts into the given `table-name` with the given `item`. The
  table-name and item structure are defined in the `database-schema`."
  [conn table-name item & {:keys [ignore-conflict-on]}]
  (safe-go
    (if (get-table-schema table-name)
      (let [table-column-names (set (get-table-column-names table-name))
            auto-increment-columns (get-table-columns-by-type table-name auto-increment-types)
            ;; remove auto increment columns that are nil in the item
            item (select-keys item (set/difference

                                     table-column-names

                                     (->> auto-increment-columns
                                          (filter (fn [col] (nil? (get item col)))))))
            statement {:insert-into table-name
                       :columns (keys item)
                       :values [(->> (vals item)
                                     (map #(if (keyword? %) (name %) %)))]
                       :returning [:*]}
            final-statement (if ignore-conflict-on
                              (merge statement {:on-conflict ignore-conflict-on :do-nothing []})
                              statement)]
        (not-empty (try
                     (first (<? (db/run! conn final-statement)))
                     (catch js/Error e
                       (log/error "Error executing insert statement" {:error e
                                                                      :statement final-statement})))))
      (log/error (str/format "Unable to find table schema for '%s'" table-name)))))


(defn update-row!
  "Updates the given `table-name` with the given `item`. The table-name
  and item structure are defined in the `database-schema`.

  Notes:

  - The :id-keys within the table-schema needs to be defined with at
  least one id-key in order to correctly update a row.
  "
  [conn table-name item]
  (safe-go
    (if-let [table-schema (get-table-schema table-name)]
      (do
        (assert (seq (:id-keys table-schema))
                (str/format ":id-keys for table schema '%s' is required for updating rows." table-name))
        (let [table-column-names (get-table-column-names table-name)
              item (select-keys item table-column-names)
              statement {:update table-name
                         :set item
                         :where (concat
                                  [:and]
                                  (for [id-key (:id-keys table-schema)]
                                    [:= id-key (get item id-key)]))
                         :returning [:*]}]
          (not-empty (try
                       (<? (db/run! conn statement))
                       (catch js/Error e
                         (log/error "Error executing update statement" {:error e :statement statement})
                         (throw e))))))
      (log/error (str/format "Unable to find table schema for '%s'" table-name)))))


(defn get-row
  "Get the given `fields` for the data row described by the `table-name`
  and `item` containing appropriate `item-keys` to identify the item.

  Notes:

  - The :id-keys within the table-schema need to be defined.

  - If `fields` are not supplied, all of the table columns are returned.
  "
  [conn table-name item & fields]
  (safe-go
    (if-let [table-schema (get-table-schema table-name)]
      (do
        (assert (seq (:id-keys table-schema))
                (str/format ":id-keys for table schema '%s' is required for getting rows." table-name))
        (let [table-column-names (get-table-column-names table-name)
              item (select-keys item table-column-names)
              fields (or fields table-column-names)]
          (not-empty (<? (db/get conn {:select fields
                                       :from [table-name]
                                       :where (concat
                                                [:and]
                                                (for [id-key (:id-keys table-schema)]
                                                  [:= id-key (get item id-key)]))})))))
      (log/error (str/format "Unable to find table schema for '%s'" table-name)))))


(defn get-list
  "Get a list of rows with the given `fields` for the given `table-name`.
  The `item` should contain appropriate `list-keys` to identify the
  row listing. If no fields are supplied, it is assumed that you want
  *all* row columns for each row.

  Notes:

  - The :list-keys within the table-schema need to be defined.

  - If the :list-keys sequence is empty, assumed that every row is
  returned.
  "
  [conn table-name item & fields]
  (safe-go
    (if-let [table-schema (get-table-schema table-name)]
      (do
        (assert (sequential? (:list-keys table-schema))
                (str/format ":list-keys for table schema '%s' is required for getting rows." table-name))
        (let [table-column-names (get-table-column-names table-name)
              item (select-keys item table-column-names)
              fields (or fields table-column-names)
              list-keys (:list-keys table-schema)
              where-clause (if (> (count list-keys) 0)
                             (concat
                               [:and]
                               (for [list-key list-keys]
                                 [:= list-key (get item list-key)]))
                             [:= 1 1])]
          (not-empty (<? (db/all conn {:select fields
                                       :from [table-name]
                                       :where where-clause})))))
      (log/error (str/format "Unable to find table schema for '%s'" table-name)))))


;;
;; Application level db access ;;
;;
(defn- add-new-associations
  "Returns map suitablefor honeysql with upsert semantics to store associated model.
  Association is identified by address (foreign key)"
  [address table column values]
  (let [fk-column :user/id
        value-tuples (map #(into [address %]) values)]
    {:insert-into table
     :columns [fk-column column]
     :values value-tuples
     :on-conflict [fk-column column]
     :do-update-set [column]}))


(defn- remove-old-associations
  "Deletes rows identified by address"
  [address table]
  (let [fk-column :user/id]
    {:delete-from table :where [:= fk-column address]}))


(defn- add-missing-values
  "Helper to populate tables with normalized values (e.g. languages, skills, categories)
  when new value is seen for the first time. In the future may get replaced with some
  initial data loading script"
  [table values]
  {:insert-into table :values (map vector values) :on-conflict nil :do-nothing []})


(defn update-associated-values
  [conn user-id [pk-table target-table column values]]
  (safe-go
    (when-not (nil? pk-table) (<? (db/run! conn (add-missing-values pk-table values))))
    (<? (db/run! conn (remove-old-associations user-id target-table)))
    (<? (db/run! conn (add-new-associations user-id target-table column values)))))


(defn upsert-user
  [conn user]
  (let [values (select-keys user (get-table-column-names :Users))
        target [nil :UserLanguage :language/id (:user/languages user)]]
    (safe-go
      (<? (db/run! conn
                   {:insert-into :Users,
                    :values [values]
                    :upsert
                    (array-map :on-conflict [:user/id]
                               :do-update-set (keys values))})))
    (update-associated-values conn (:user/id user) target)))


(defn upsert-candidate
  [conn user]
  (let [candidate (select-keys user (get-table-column-names :Candidate))]
    (safe-go
      (<? (db/run! conn {:insert-into :Candidate
                         :values [candidate]
                         :upsert (array-map :on-conflict [:user/id]
                                            :do-update-set (keys candidate))}))
      (doseq [address [(:user/id user)]
              target [[:Category :CandidateCategory :category/id (:candidate/categories user)]
                      [:Skill :CandidateSkill :skill/id (:candidate/skills user)]]]
        (update-associated-values conn address target)))))


(defn upsert-employer
  [conn user]
  (safe-go
    (let [employer (select-keys user (get-table-column-names :Employer))]
      (<? (db/run! conn {:insert-into :Employer
                         :values [employer]
                         :upsert (array-map :on-conflict [:user/id]
                                            :do-update-set (keys employer))})))))


(defn upsert-arbiter
  [conn user]
  (let [arbiter (select-keys user (get-table-column-names :Arbiter))]
    (safe-go
      (<? (db/run! conn {:insert-into :Arbiter
                         :values [arbiter]
                         :upsert (array-map :on-conflict [:user/id]
                                            :do-update-set (keys arbiter))})))))


(defn upsert-user!
  [conn {:keys [user candidate employer arbiter]}]
  (safe-go
    (when user (upsert-user conn user))
    (when candidate (upsert-candidate conn candidate))
    (when employer (upsert-employer conn employer))
    (when arbiter (upsert-arbiter conn arbiter))))


;; (defn upsert-user! [conn {:user/keys [type] :as user}]
;;   (safe-go
;;    (let [values (select-keys user (get-table-column-names :Users))
;;          _ (<? (db/run! conn
;;                         {:insert-into :Users,
;;                          :values [values]
;;                          :upsert
;;                          (array-map :on-conflict [:user/id]
;;                                     :do-update-set (keys values))}))]
;;      (case type
;;        :arbiter (let [arbiter (select-keys user (get-table-column-names :Arbiter))]
;;                   (<? (db/run! conn {:insert-into :Arbiter
;;                                      :values [arbiter]
;;                                      :upsert (array-map :on-conflict [:user/id]
;;                                                         :do-update-set (keys arbiter))})))
;;        :employer (let [employer (select-keys user (get-table-column-names :Employer))]
;;                    (<? (db/run! conn {:insert-into :Employer
;;                                       :values [employer]
;;                                       :upsert (array-map :on-conflict [:user/id]
;;                                                          :do-update-set (keys employer))})))
;;        :candidate (let [candidate (select-keys user (get-table-column-names :Candidate))]
;;                     (<? (db/run! conn {:insert-into :Candidate
;;                                        :values [candidate]
;;                                        :upsert (array-map :on-conflict [:user/id]
;;                                                           :do-update-set (keys candidate))}))
;;                     (doseq [address [(:user/id user)]
;;                             target [[:Category :CandidateCategory :category/id (:candidate/categories user)]
;;                                     [:Skill :CandidateSkill :skill/id (:candidate/skills user)]
;;                                     [nil :UserLanguage :language/id (:user/languages user)]]]
;;                       (let [[pk-table table column values] target]
;;                         (if-not (nil? pk-table) (<? (db/run! conn (add-missing-values pk-table values))))
;;                         (<? (db/run! conn (remove-old-associations address table)))
;;                         (<? (db/run! conn (add-new-associations address table column values))))))))))

(defn upsert-user-social-accounts!
  [conn user-social-accounts]
  (safe-go
    (let [values (select-keys user-social-accounts (get-table-column-names :UserSocialAccounts))]
      (<? (db/run! conn
                   {:insert-into :UserSocialAccounts,
                    :values [values]
                    :upsert
                    (array-map :on-conflict [:user/id]
                               :do-update-set (keys values))})))))


(defn add-skills
  [conn job-id skills]
  (safe-go
    (doseq [skill skills]
      (<? (insert-row! conn :JobSkill {:job/id job-id :skill/id skill})))))


(defn add-job-arbiter
  [conn job-id user-address]
  (safe-go
    (<? (insert-row! conn :JobArbiter {:job/id job-id
                                       :user/id user-address
                                       :job-arbiter/status "invited"
                                       :job-arbiter/date-created (.now js/Date)}))))


(defn update-arbitration
  [conn params]
  (safe-go
    (<? (update-row! conn :JobArbiter params))))


(defn add-job
  [conn job]
  (safe-go
    (let [skills (:job/required-skills job)
          job-id-from-ipfs (:job/id job)
          job-exists-query {:select [(sql/call :exists {:select [1] :from [:Job] :where [:= :Job.job/id job-id-from-ipfs]})]}
          job-exists? (:exists (<? (db/get conn job-exists-query)))]
      (if job-exists?
        (log/info (str "Not adding job because already exists with :job/id " job-id-from-ipfs))
        (do
          (<? (insert-row! conn :Job job))
          (add-skills conn job-id-from-ipfs skills)
          (doseq [arbiter (:invited-arbiters job)]
            (<? (add-job-arbiter conn job-id-from-ipfs arbiter))))))))


(defn update-job
  [conn job-id job-data]
  (safe-go
    (<? (update-row! conn :Job (assoc job-data :job/id job-id)))))


(defn update-job-story
  [conn job-story-id job-story-data]
  (safe-go
    (<? (update-row! conn :JobStory (assoc job-story-data :job-story/id job-story-id)))))


(defn get-invoice-message
  [conn job-story-id invoice-id]
  (safe-go
    (<? (db/get conn {:select [:*]
                      :from [:JobStoryInvoiceMessage]
                      :where [:and
                              [:= :job-story/id job-story-id]
                              [:= :invoice/ref-id invoice-id]]}))))


(defn update-job-story-invoice-message
  [conn msg]
  (safe-go
    (<? (update-row! conn :JobStoryInvoiceMessage msg))))


(defn add-message
  "Inserts a Message. Returns autoincrement id"
  [conn message]
  (safe-go
    (let [msg-id (-> (<? (insert-row! conn :Message message))
                     :message/id)
          story-status (case (:job-story-message/type message)
                         :proposal "proposal"
                         :invitation "invitation"
                         "created")
          job-story-common-fields {:job/id (:job/id message)
                                   :job-story/date-created (:message/date-created message)
                                   :job-story/status story-status
                                   :job-story/proposal-rate (:job-story/proposal-rate message)}
          job-story-params (case (:job-story-message/type message)
                             :proposal (merge job-story-common-fields {:job-story/proposal-message-id msg-id
                                                                       :job-story/candidate (:candidate message)})
                             :invitation (merge job-story-common-fields {:job-story/invitation-message-id msg-id
                                                                         :job-story/candidate (:candidate message)})
                             job-story-common-fields)
          job-story-id (or (:job-story/id message)
                           (:job-story/id (<? (insert-row! conn :JobStory job-story-params))))
          message (assoc message :message/id msg-id :job-story/id job-story-id)]
      (case (:message/type message)
        :job-story-message
        (do
          (<? (insert-row! conn :JobStoryMessage (assoc message
                                                        :message/id msg-id
                                                        :job-story/id job-story-id)))
          (<? (case (:job-story-message/type message)
                :raise-dispute
                (update-job-story-invoice-message conn {:job-story/id job-story-id
                                                        :message/id (:message/id (<? (get-invoice-message conn job-story-id (:invoice/id message))))
                                                        :invoice/dispute-raised-message-id msg-id
                                                        :invoice/status "dispute-raised"})

                :resolve-dispute (update-job-story-invoice-message conn
                                                                   {:job-story/id job-story-id
                                                                    :message/id (:message/id (<? (get-invoice-message conn job-story-id (:invoice/id message))))
                                                                    :invoice/dispute-resolved-message-id msg-id
                                                                    :invoice/status "dispute-resolved"})
                :proposal (update-row! conn :JobStory (assoc message
                                                             :job-story/id (:job-story/id message)
                                                             :job-story/proposal-message-id msg-id))
                :invitation (update-row! conn :JobStory (assoc message
                                                               :job-story/id (:job-story/id message)
                                                               :job-story/invitation-message-id msg-id))
                :accept-proposal (update-row! conn :JobStory (assoc message
                                                                    :job-story/status "active"
                                                                    :job-story/candidate (:candidate message)
                                                                    :job-story/id (:job-story/id message)
                                                                    :job-story/date-contract-active (:message/date-created message)))
                :accept-invitation (update-row! conn :JobStory (assoc message
                                                                      :job-story/status "active"
                                                                      :job-story/id (:job-story/id message)
                                                                      :job-story/date-contract-active (:message/date-created message)))
                :invoice (insert-row! conn :JobStoryInvoiceMessage message)
                :payment (update-job-story-invoice-message conn
                                                           {:job-story/id job-story-id
                                                            :message/id (:message/id (<? (get-invoice-message conn job-story-id (:invoice/id message))))
                                                            :invoice/payment-message-id msg-id
                                                            :invoice/status "paid"})
                :feedback  (insert-row! conn :JobStoryFeedbackMessage message))))

        :direct-message
        (<? (insert-row! conn :DirectMessage message)))
      {:job-story/id job-story-id :message/id msg-id})))


(defn add-job-story
  "Inserts a JobStory. Returns autoincrement id"
  [conn job-story]
  (safe-go
    (:job-story/id (<? (insert-row! conn :JobStory job-story)))))


(defn add-job-story-message
  [conn job-story-message]
  (safe-go
    (<? (insert-row! conn :JobStoryMessage job-story-message))))


(defn add-message-file
  [conn message-id file]
  (safe-go
    (let [{:file/keys [id]} (<? (insert-row! conn :File file))]
      (<? (insert-row! conn :MessageFile {:message/id message-id
                                          :file/id id})))))


(defn update-job-candidate
  [conn job-id user-address]
  (safe-go
    (<? (update-row! conn :EthlanceJob {:ethlance-job/id job-id
                                        :ethlance-job/candidate user-address}))))


(defn get-job-story-id-by-job-id
  [conn job-id]
  (safe-go
    (:id (<? (db/get conn {:select [[:js.job-story/id :id]]
                           :from [[:JobStory :js]]
                           :join [[:Job :j] [:= :js.job/id :j.job/id]]
                           :where [:ilike :j.job/id job-id]})))))


(defn get-candidate-id-by-job-story-id
  [conn job-story-id]
  (safe-go
    (:id (<? (db/get conn {:select [[:job-story/candidate :id]]
                           :from [:JobStory]
                           :where [:= :job-story/id job-story-id]})))))


(defn get-employer-id-by-job-story-id
  [conn job-story-id]
  (safe-go
    (:id (<? (db/get conn {:select [[:Job.job/creator :id]]
                           :from [:JobStory]
                           :join [:Job [:= :Job.job/id :JobStory.job/id]]
                           :where [:= :job-story/id job-story-id]})))))


(defn get-arbiter-id-by-job-story-id
  [conn job-story-id]
  (safe-go
    (:id (<? (db/get conn {:select [[:JobArbiter.user/id :id]]
                           :from [:JobStory]
                           :join [:Job [:= :Job.job/id :JobStory.job/id]
                                  :JobArbiter [:= :Job.job/id :JobArbiter.job/id]]
                           :where [:and
                                   [:= :job-story/id job-story-id]
                                   [:= :job-arbiter/status "accepted"]]})))))


(defn update-job-story-status
  [conn job-story-id status]
  (safe-go
    (<? (db/get conn {:update :JobStory
                      :set {:job-story/status status}
                      :where [:= :job-story/id job-story-id]}))))


(defn set-job-story-invoice-status-for-job
  [conn job-id invoice-id status]
  (safe-go
    (let [job-story-id (<? (get-job-story-id-by-job-id conn job-id))]
      (<? (db/run! conn {:update :JobStoryInvoiceMessage
                         :set {:invoice/status status}
                         :where [:and
                                 [:= :job-story/id job-story-id]
                                 [:= :invoice/ref-id invoice-id]]})))))


(defn add-contribution
  [conn job-id contributor-address contribution-id amount]
  (safe-go
    (<? (insert-row! conn :JobContribution {:job/id job-id
                                            :user/id contributor-address
                                            :job-contribution/amount amount
                                            :job-contribution/id contribution-id}))))


(defn get-token
  [conn token-address]
  (safe-go
    (<? (db/get conn {:select [:*]
                      :from [:TokenDetail]
                      :where [:= :TokenDetail.token-detail/id token-address]}))))


(defn store-token-details
  [conn token-details]
  (safe-go
    (<? (insert-row! conn :TokenDetail
                     {:token-detail/id (:address token-details)
                      :token-detail/type (:type token-details)
                      :token-detail/name (:name token-details)
                      :token-detail/symbol (:symbol token-details)
                      :token-detail/decimals (:decimals token-details)}))))


(defn load-processed-events-checkpoint
  [callback]
  (.then
    (district.server.async-db/get-connection)
    (fn [conn]
      (let [result-chan (db/get conn {:select [:*]
                                      :from [:ContractEventCheckpoint]
                                      :order-by [[:created-at :desc]]})]
        (take! result-chan (fn [result] (callback nil (clojure.walk/keywordize-keys (get result :checkpoint)))))))))


(defn save-processed-events-checkpoint
  [checkpoint & [callback]]
  (.then
    (district.server.async-db/get-connection)
    (fn [conn]
      (let [result-chan (db/run! conn {:insert-into :ContractEventCheckpoint
                                       :values [{:checkpoint (.stringify js/JSON (clj->js checkpoint))
                                                 :created-at (new js/Date)}]})]
        (when (fn? callback) (take! result-chan callback))))))


(defn ready-state?
  []
  (go-loop []
    (let [ready? (= :db/ready @db-state)]
      (log/info "Polling..." {:ready? ready?})
      (if ready?
        (do
          (log/info "DB in ready state. Returning")
          @db-state)
        (do
          (<! (async/timeout 1000))
          (recur))))))


(defn start
  "Start the ethlance-db mount component."
  [{:keys [resync?] :as opts}]
  (safe-go
    (let [conn (<? (db/get-connection))]
      (log/info "Starting Ethlance DB component" opts)
      (when resync?
        (log/info "Database module called with a resync flag.")
        (<? (drop-db! conn)))
      (<? (create-db! conn))
      (reset! db-state :db/ready)
      (log/info "Ethlance DB component started")
      @db-state)))


(defn stop
  "Stop the ethlance-db mount component."
  []
  (reset! db-state :db/stopped)
  @db-state)
