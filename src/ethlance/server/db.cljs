(ns ethlance.server.db
  "Represents the ethlance in-memory sqlite database. Contains a mount
  component for creating the in-memory database upon initial load."
  (:require
   [clojure.pprint :as pprint]
   [com.rpl.specter :as $ :include-macros true]
   [cuerdas.core :as str]
   [district.server.config :refer [config]]
   [district.server.db :as db]
   [district.server.db.column-types :refer [address not-nil default-nil default-zero default-false sha3-hash primary-key]]
   [district.server.db.honeysql-extensions]
   [honeysql.core :as sql]
   [honeysql.helpers :as sqlh :refer [merge-where merge-order-by merge-left-join defhelper]]
   [medley.core :as medley]
   [mount.core :as mount :refer [defstate]]
   [taoensso.timbre :as log]))


(declare start stop)


(def mount-state-key
  "Key defining our mount component within the district configuration"
  :ethlance/db)


(defstate ^{:on-reload :noop} ethlance-db
  :start (start (merge (get @config mount-state-key)
                       (mount-state-key (mount/args))))
  :stop (stop))


(def database-schema
  "Represents the database schema, consisting of tables, and their
  column descriptions.

  Notes:

  - Table Entry order matters for creation and deletion.

  - :id-keys is a listing which makes up a table's compound key

  - :list-keys is a listing which makes up a table's key for producing
  a proper listing.
  "
  ;;
  ;; Ethlance Tables
  ;;
  [{:table-name :User
    :table-columns
    [[:user/address address]
     [:user/type :varchar not-nil]
     [:user/country-code :varchar #_not-nil]
     [:user/user-name :varchar]
     [:user/full-name :varchar]
     [:user/email :varchar not-nil]
     [:user/profile-image :varchar]
     [:user/date-registered :unsigned :integer]
     [:user/date-updated :unsigned :integer]
     [:user/github-username :varchar]
     [:user/linkedin-username :varchar]
     [:user/status :varchar]
     ;; PK
     [(sql/call :primary-key :user/address)]]
    :list-keys []}

   {:table-name :Candidate
    :table-columns
    [[:user/address address]
     [:candidate/bio :varchar]
     [:candidate/professional-title :varchar]
     [:candidate/rate :integer not-nil]
     [:candidate/rate-currency-id :varchar not-nil]
     ;; PK
     [(sql/call :primary-key :user/address)]
     ;; FKs
     [(sql/call :foreign-key :user/address) (sql/call :references :User :user/address) (sql/raw "ON DELETE CASCADE")]]
    :list-keys []}

   {:table-name :Employer
    :table-columns
    [[:user/address address not-nil]
     [:employer/bio :varchar]
     [:employer/professional-title :varchar]
     ;; PK
     [(sql/call :primary-key :user/address)]
     ;; FKs
     [(sql/call :foreign-key :user/address) (sql/call :references :User :user/address) (sql/raw "ON DELETE CASCADE")]]
    :list-keys []}

   {:table-name :Arbiter
    :table-columns
    [[:user/address address not-nil]
     [:arbiter/bio :varchar]
     [:arbiter/professional-title :varchar]
     [:arbiter/fee :integer not-nil]
     [:arbiter/fee-currency-id :varchar not-nil]
     ;; PK
     [(sql/call :primary-key :user/address)]
     ;; FKs
     [(sql/call :foreign-key :user/address) (sql/call :references :User :user/address) (sql/raw "ON DELETE CASCADE")]]
    :list-keys []}

   {:table-name :UserLanguage
    :table-columns
    [[:user/address address not-nil]
     [:language/id :varchar not-nil]
     ;; PK
     [(sql/call :primary-key :user/address :language/id)]
     ;; FKs
     [(sql/call :foreign-key :user/address) (sql/call :references :User :user/address) (sql/raw "ON DELETE CASCADE")]]
    :list-keys []}

   {:table-name :Category
    :table-columns
    [[:category/id :varchar not-nil]
     ;; PK
     [(sql/call :primary-key :category/id)]]
    :list-keys []}

   {:table-name :Skill
    :table-columns
    [[:skill/id :varchar not-nil]
     ;; PK
     [(sql/call :primary-key :skill/id)]]
    :list-keys []}

   {:table-name :ArbiterCategory
    :table-columns
    [[:user/address address not-nil]
     [:category/id :varchar not-nil]
     ;; PK
     [(sql/call :primary-key :user/address :category/id)]
     ;; FKs
     [(sql/call :foreign-key :user/address) (sql/call :references :User :user/address) (sql/raw "ON DELETE CASCADE")]
     [(sql/call :foreign-key :category/id) (sql/call :references :Category :category/id) (sql/raw "ON DELETE CASCADE")]]
    :list-keys []}

   {:table-name :ArbiterSkill
    :table-columns
    [[:user/address address not-nil]
     [:skill/id :varchar not-nil]
     ;; PK
     [(sql/call :primary-key :user/address :skill/id)]
     ;; FKs
     [(sql/call :foreign-key :user/address) (sql/call :references :User :user/address) (sql/raw "ON DELETE CASCADE")]
     [(sql/call :foreign-key :skill/id) (sql/call :references :Skill :skill/id) (sql/raw "ON DELETE CASCADE")]]
    :list-keys []}

   {:table-name :CandidateCategory
    :table-columns
    [[:user/address address not-nil]
     [:category/id :varchar not-nil]
     ;; PK
     [(sql/call :primary-key :user/address :category/id)]
     ;; FKs
     [(sql/call :foreign-key :user/address) (sql/call :references :User :user/address) (sql/raw "ON DELETE CASCADE")]
     [(sql/call :foreign-key :category/id) (sql/call :references :Category :category/id) (sql/raw "ON DELETE CASCADE")]]
    :list-keys []}

   {:table-name :CandidateSkill
    :table-columns
    [[:user/address address not-nil]
     [:skill/id :varchar not-nil]
     ;; PK
     [(sql/call :primary-key :user/address :skill/id)]
     ;; FKs
     [(sql/call :foreign-key :user/address) (sql/call :references :User :user/address) (sql/raw "ON DELETE CASCADE")]
     [(sql/call :foreign-key :skill/id) (sql/call :references :Skill :skill/id) (sql/raw "ON DELETE CASCADE")]]
    :list-keys []}

   {:table-name :Job
    :table-columns
    [[:job/id :integer]
     [:job/type :varchar not-nil]
     [:job/title :varchar not-nil]
     [:job/description :varchar not-nil]
     [:job/category :varchar]
     [:job/status :varchar]
     [:job/date-created :unsigned :integer]
     [:job/date-published :unsigned :integer]
     [:job/date-updated :unsigned :integer]
     [:job/expertise-level :integer]
     [:job/token address]
     [:job/token-version :integer]
     [:job/reward :unsigned :integer]
     [:job/web-reference-url :varchar]
     [:job/language-id :varchar]
     ;; PK
     [(sql/call :primary-key :job/id)]]
    :list-keys []}

   {:table-name :EthlanceJob
    :table-columns
    [[:job/id :integer]
     [:ethlance-job/id :integer]
     [:ethlance-job/estimated-lenght :integer]
     [:ethlance-job/max-number-of-candidates :integer]
     [:ethlance-job/invitation-only? :integer]
     [:ethlance-job/required-availability :integer]
     [:ethlance-job/hire-address :varchar]
     [:ethlance-job/bid-option :integer]

     ;; PK
     [(sql/call :primary-key :job/id)]

     ;; FK
     [(sql/call :foreign-key :job/id) (sql/call :references :Job :job/id) (sql/raw "ON DELETE CASCADE")]
     ;; TODO: add to  candidate table
     ]}

   {:table-name :StandardBounty
    :table-columns
    [[:job/id :integer]
     [:standard-bounty/id :integer]
     [:standard-bounty/platform :varchar]
     [:standard-bounty/deadline :integer]
     ;; PK
     [(sql/call :primary-key :job/id)]

     ;; FK
     [(sql/call :foreign-key :job/id) (sql/call :references :Job :job/id) (sql/raw "ON DELETE CASCADE")]
     ]}

   {:table-name :JobCreator
    :table-columns
    [[:job/id :integer]
     [:user/address address]
     ;; PK
     [(sql/call :primary-key :job/id :user/address)]
     ;; FKs
     [(sql/call :foreign-key :user/address) (sql/call :references :User :user/address) (sql/raw "ON DELETE CASCADE")]
     [(sql/call :foreign-key :job/id) (sql/call :references :Job :job/id) (sql/raw "ON DELETE CASCADE")]]
    :list-keys []}

   {:table-name :JobContribution
    :table-columns
    [[:job/id :integer]
     [:user/address address]
     [:job-contribution/amount :integer]
     [:job-contribution/id :integer]

     ;; PK
     [(sql/call :primary-key :job/id :user/address)]

     ;; FKs
     [(sql/call :foreign-key :job/id) (sql/call :references :Job :job/id) (sql/raw "ON DELETE CASCADE")]
     ]}

   {:table-name :JobSkill
    :table-columns
    [[:job/id :integer]
     [:skill/id :varchar]
     ;; PK
     [(sql/call :primary-key :job/id :skill/id)]
     ;; FKs
     [(sql/call :foreign-key :job/id) (sql/call :references :Job :job/id) (sql/raw "ON DELETE CASCADE")]
     [(sql/call :foreign-key :skill/id) (sql/call :references :Skill :skill/id) (sql/raw "ON DELETE CASCADE")]]
    :list-keys []}

   {:table-name :JobArbiter
    :table-columns
    [[:job/id :integer]
     [:user/address address]
     [:job-arbiter/fee :unsigned :integer]
     [:job-arbiter/fee-currency-id :varchar]
     [:job-arbiter/status :varchar]
     ;; PK
     [(sql/call :primary-key :job/id :user/address)]

     ;; FKs
     [(sql/call :foreign-key :user/address) (sql/call :references :User :user/address) (sql/raw "ON DELETE CASCADE")]
     [(sql/call :foreign-key :job/id) (sql/call :references :Job :job/id) (sql/raw "ON DELETE CASCADE")]]
    :list-keys []}

   {:table-name :JobFile
    :table-columns
    [[:job/id :integer]
     [:job/file-id :integer]

     ;; FKs
     [(sql/call :foreign-key :job/id) (sql/call :references :Job :job/id) (sql/raw "ON DELETE CASCADE")]
     ]
    :list-keys []}

   {:table-name :JobStory
    :table-columns
    [[:job-story/id :integer]
     [:job/id :integer]
     [:job-story/status :varchar]
     [:job-story/date-created :unsigned :integer]
     [:job-story/date-updated :unsigned :integer]
     [:job-story/invitation-message-id :integer]
     [:job-story/proposal-message-id :integer]
     [:job-story/raised-dispute-message-id :integer]
     [:job-story/resolved-dispute-message-id :integer]
     [:job-story/proposal-rate :integer]
     [:job-story/proposal-rate-currency-id :varchar]
     ;; PK
     [(sql/call :primary-key :job-story/id)]

     ;; FKs
     [(sql/call :foreign-key :job/id) (sql/call :references :Job :job/id) (sql/raw "ON DELETE CASCADE")]
     [(sql/call :foreign-key :job-story/invitation-message-id) (sql/call :references :Message :message/id) (sql/raw "ON DELETE CASCADE")]
     [(sql/call :foreign-key :job-story/proposal-message-id) (sql/call :references :Message :message/id) (sql/raw "ON DELETE CASCADE")]]
    :list-keys []}

   {:table-name :EthlanceJobStory
    :table-columns
    [[:job-story/id :integer]
     [:ethlance-job-story/invitation-message-id :integer]
     [:ethlance-job-story/proposal-message-id :integer]
     [:ethlance-job-story/proposal-rate :integer]
     [:ethlance-job-story/proposal-rate-currency-id :varchar]
     [:ethlance-job-story/candidate :varchar]

     ;; PK
     [(sql/call :primary-key :job-story/id)]

     ;; FKs
     [(sql/call :foreign-key :job-story/id) (sql/call :references :JobStory :job-story/id) (sql/raw "ON DELETE CASCADE")]]
    }

   {:table-name :JobStoryMessage
    :table-columns
    [[:job-story/id :integer]
     [:message/id :integer]
     ;; PK
     [(sql/call :primary-key :job-story/id :message/id)]
     ;; FKs
     [(sql/call :foreign-key :job-story/id) (sql/call :references :JobStory :job-story/id) (sql/raw "ON DELETE CASCADE")]
     [(sql/call :foreign-key :message/id) (sql/call :references :Message :message/id) (sql/raw "ON DELETE CASCADE")]]
    :list-keys []}

   {:table-name :JobStoryInvoiceMessage
    :table-columns
    [
     [:job-story/id :integer]
     [:message/id :integer]
     [:invoice/status :varchar]
     [:invoice/amount-requested :unsigned :integer]
     [:invoice/amount-paid :unsigned :integer]
     [:invoice/date-paid :unsigned :integer]
     [:invoice/date-work-started :unsigned :integer]
     [:invoice/date-work-ended :unsigned :integer]
     [:invoice/work-duration :unsigned :integer]
     [:invoice/ref-id :integer]
     ;; PK
     [(sql/call :primary-key :job-story/id :message/id)]
     ;; FKs
     [(sql/call :foreign-key :job-story/id) (sql/call :references :JobStory :job-story/id) (sql/raw "ON DELETE CASCADE")]
     [(sql/call :foreign-key :message/id) (sql/call :references :Message :message/id) (sql/raw "ON DELETE CASCADE")]]
    :list-keys []}

   {:table-name :JobStoryFeedbackMessage
    :table-columns
    [[:job-story/id :integer not-nil]
     [:message/id :integer not-nil]
     [:feedback/rating :integer not-nil]
     [:user/address :varchar not-nil]

     ;; PK
     [(sql/call :primary-key :job-story/id :message/id)]
     ;; FKs
     [(sql/call :foreign-key :job-story/id) (sql/call :references :JobStory :job-story/id) (sql/raw "ON DELETE CASCADE")]
     [(sql/call :foreign-key :user/address) (sql/call :references :User :user/address) (sql/raw "ON DELETE CASCADE")]
     [(sql/call :foreign-key :message/id) (sql/call :references :Message :message/id) (sql/raw "ON DELETE CASCADE")]]
    :list-keys []}

   {:table-name :Message
    :table-columns
    [[:message/id :integer]
     [:message/creator address]
     [:message/text :varchar]
     [:message/date-created :unsigned :integer]
     ;; proposal, invitation, raised dispute, resolved dispute, feedback, invoice, direct message, job story message
     [:message/type :varchar not-nil]
     ;; PK
     [(sql/call :primary-key :message/id)]
     ;; FKs
     [(sql/call :foreign-key :message/id) (sql/call :references :Message :message/id) (sql/raw "ON DELETE CASCADE")]]
    :list-keys []}

   {:table-name :DirectMessage
    :table-columns
    [[:message/id :integer]
     [:direct-message/receiver address]
     [:direct-message/read? :integer]
     ;; PK
     [(sql/call :primary-key :message/id)]
     ;; FKs
     [(sql/call :foreign-key :message/id) (sql/call :references :Message :message/id) (sql/raw "ON DELETE CASCADE")]]

    :list-keys []}

   {:table-name :MessageFile
    :table-columns
    [[:message/id :integer]
     [:file/id :integer]

     ;; PK
     [(sql/call :primary-key :message/id :file/id)]
     ;; FKs
     [(sql/call :foreign-key :message/id) (sql/call :references :Message :message/id) (sql/raw "ON DELETE CASCADE")]
     [(sql/call :foreign-key :file/id) (sql/call :references :File :file/id) (sql/raw "ON DELETE CASCADE")]]
    :list-keys []}

   {:table-name :File
    :table-columns
    [[:file/id :integer]
     [:file/hash :varchar]
     [:file/name :varchar]
     [:file/directory-hash :varchar]

     ;; PK
     [(sql/call :primary-key :file/id)]
     ]
    :list-keys []}

   {:table-name :ReplayEventQueue
    :table-columns
    [[:event/comparable-id :integer]
     [:event/string :varchar]

     [(sql/call :primary-key :event/comparable-id)]]}])


(defn list-tables
  "Lists all of the tables currently in the sqlite3 database."
  []
  (let [results
        (db/all {:select [:name]
                 :from [:sqlite_master]
                 :where [:= :type "table"]})]
    (mapv :name results)))

(defn print-db
  "(print-db) prints all db tables to the repl
   (print-db :users) prints only users table"
  ([] (print-db nil))
  ([table]
   (let [select (fn [& [select-fields & r]]
                  (pprint/print-table (db/all (->> (partition 2 r)
                                                   (map vec)
                                                   (into {:select select-fields})))))
         all-tables (if table
                      [(name table)]
                      (->> (db/all {:select [:name] :from [:sqlite-master] :where [:= :type "table"]})
                           (map :name)))]
     (doseq [t all-tables]
       (println "#######" (str/upper t) "#######")
       (select [:*] :from [(keyword t)])))))

(defn table-exists?
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


(defn- get-table-column-names
  "Retrieves the table column names for a given table schema defined by their `table-name`."
  [table-name]
  (let [table-schema (get-table-schema table-name)]
    (->> (:table-columns table-schema)
         (map first)
         (filter keyword?))))


(defn create-db!
  "Creates the database with tables defined in the `database-schema`."
  []
  (log/info "Creating Sqlite Database...")
  (doseq [{:keys [table-name table-columns]} database-schema]
    (log/debug (str/format "  - Creating Database Table '%s' ..." table-name))
    (db/run! {:create-table [table-name :if-not-exists] :with-columns [table-columns]}))
  (log/debug "Tables Created: " (list-tables)))


(defn drop-db!
  "Drops all of the database tables defined in the `database-schema`."
  []
  (log/info "Dropping Sqlite Database...")
  (doseq [{:keys [table-name]} (reverse database-schema)]
    (log/debug (str/format "  - Dropping Database Table '%s' ..." table-name))
    (db/run! {:drop-table [:if-exists table-name]})))

(defn insert-row!
  "Inserts into the given `table-name` with the given `item`. The
  table-name and item structure are defined in the `database-schema`."
  [table-name item]
  (if-let [table-schema (get-table-schema table-name)]
    (let [table-column-names (get-table-column-names table-name)
          item (select-keys item table-column-names)
          statement {:insert-into table-name
                     :columns (keys item)
                     :values [(->> (vals item)
                                   (map #(if (keyword? %) (name %) %)))]}]
      (not-empty (try
                   (db/run! statement)
                   (catch js/Error e
                     (log/error "Error executing insert statement" {:error e
                                                                    :statement statement})
                     (print-db)
                     ))))
    (log/error (str/format "Unable to find table schema for '%s'" table-name))))


(defn update-row!
  "Updates the given `table-name` with the given `item`. The table-name
  and item structure are defined in the `database-schema`.

  Notes:

  - The :id-keys within the table-schema needs to be defined with at
  least one id-key in order to correctly update a row.
  "
  [table-name item]
  (if-let [table-schema (get-table-schema table-name)]
    (do
      (assert (not (empty? (:id-keys table-schema)))
              (str/format ":id-keys for table schema '%s' is required for updating rows." table-name))
      (let [table-column-names (get-table-column-names table-name)
            item (select-keys item table-column-names)
            statement {:update table-name
                       :set item
                       :where (concat
                               [:and]
                               (for [id-key (:id-keys table-schema)]
                                 [:= id-key (get item id-key)]))}]
        (not-empty (try
                     (db/run! statement)
                     (catch js/Error e
                       (log/error "Error executing update statement" {:error e
                                                                      :statement statement})
                       (print-db)
                       (throw e))))))
    (log/error (str/format "Unable to find table schema for '%s'" table-name))))


(defn get-row
  "Get the given `fields` for the data row described by the `table-name`
  and `item` containing appropriate `item-keys` to identify the item.

  Notes:

  - The :id-keys within the table-schema need to be defined.

  - If `fields` are not supplied, all of the table columns are returned.
  "
  [table-name item & fields]
  (if-let [table-schema (get-table-schema table-name)]
    (do
      (assert (not (empty? (:id-keys table-schema)))
              (str/format ":id-keys for table schema '%s' is required for getting rows." table-name))
      (let [table-column-names (get-table-column-names table-name)
            item (select-keys item table-column-names)
            fields (or fields table-column-names)]
        (not-empty (db/get {:select fields
                            :from [table-name]
                            :where (concat
                                    [:and]
                                    (for [id-key (:id-keys table-schema)]
                                      [:= id-key (get item id-key)]))}))))
    (log/error (str/format "Unable to find table schema for '%s'" table-name))))


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
  [table-name item & fields]
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
        (not-empty (db/all {:select fields
                            :from [table-name]
                            :where where-clause}))))
    (log/error (str/format "Unable to find table schema for '%s'" table-name))))

(defn upsert-user! [user]
  (let [values (select-keys user (get-table-column-names :User))]
    (db/run! {:insert-into :User
              :values [values]
              :upsert {:on-conflict [:user/address]
                       :do-update-set (keys values)}})
    (case (:user/type user)
      :arbiter   (let [arbiter (select-keys user (get-table-column-names :Arbiter))]
                   (db/run! {:insert-into :Arbiter
                             :values [arbiter]
                             :upsert {:on-conflict [:user/address]
                                      :do-update-set (keys arbiter)}}))
      :employer  (let [employer (select-keys user (get-table-column-names :Employer))]
                   (db/run! {:insert-into :Employer
                             :values [employer]
                             :upsert {:on-conflict [:user/address]
                                      :do-update-set (keys employer)}}))
      :candidate (let [candidate (select-keys user (get-table-column-names :Candidate))]
                   (db/run! {:insert-into :Candidate
                             :values [candidate]
                             :upsert {:on-conflict [:user/address]
                                      :do-update-set (keys candidate)}})))))

(defn get-last-insert-id []
  (:id (db/get {:select [[(sql/call :last_insert_rowid) :id]] })))

(defn add-bounty [bounty-job]
  (insert-row! :Job (assoc bounty-job
                           :job/type "standard-bounty"))
  (let [job-id (get-last-insert-id)]
    (insert-row! :StandardBounty (assoc bounty-job :job/id job-id))))

(defn add-ethlance-job [ethlance-job]
  (insert-row! :Job (assoc ethlance-job
                           :job/type "ethlance-job"))
  (let [job-id (get-last-insert-id)]
    (insert-row! :EthlanceJob (assoc ethlance-job :job/id job-id))))

(defn get-job-id-for-bounty [bounty-id]
  (-> (db/get {:select [:job/id]
               :from [[:StandardBounty :sb]]
               :where [:= :sb.standard-bounty/id bounty-id]})
      :job/id))

(defn update-bounty [bounty-id job-data]
  (let [job-id (get-job-id-for-bounty bounty-id)]
    (update-row! :StandardBounty job-data)
    (update-row! :Job (assoc job-data :job/id job-id))))

(defn get-job-id-for-ethlance-job [ethlance-job-id]
  (-> (db/get {:select [:job/id]
               :from [[:EthlanceJob :ej]]
               :where [:= :ej.ethlance-job/id ethlance-job-id]})
      :job/id))

(defn update-ethlance-job [ethlance-job-id job-data]
  (let [job-id (get-job-id-for-ethlance-job ethlance-job-id)]
    (update-row! :EthlanceJob job-data)
    (update-row! :Job (assoc job-data :job/id job-id))))

(defn update-job-story-invoice-message  [msg]
  (update-row! :JobStoryInvoiceMessage msg))

(defn add-message
  "Inserts a Message. Returns autoincrement id"
  [message]
  (println "Inserting message " message)
  (insert-row! :Message message)
  (let [msg-id (get-last-insert-id)
        message (assoc message :message/id msg-id)]
    (case (:message/type message)
      :job-story-message
      (do
        (insert-row! :JobStoryMessage (assoc message
                                             :message/id msg-id))
        (case (:job-story-message/type message)
          :raise-dispute (update-row! :JobStory (assoc message
                                                       :job-story/id (:job-story/id message)
                                                       :job-story/raised-dispute-message-id msg-id))
          :resolve-dispute (update-row! :JobStory (assoc message
                                                         :job-story/id (:job-story/id message)
                                                         :job-story/resolved-dispute-message-id msg-id))
          :proposal (update-row! :EthlanceJobStory (assoc message
                                                          :job-story/id (:job-story/id message)
                                                          :ethlance-job-story/proposal-message-id msg-id))
          :invitation (update-row! :EthlanceJobStory (assoc message
                                                            :job-story/id (:job-story/id message)
                                                            :ethlance-job-story/invitation-message-id msg-id))
          :invoice (insert-row! :JobStoryInvoiceMessage message)
          :feedback  (insert-row! :JobStoryFeedbackMessage message)
          nil))

      :direct-message
      (insert-row! :DirectMessage message))))

(defn add-job-story
  "Inserts a JobStory. Returns autoincrement id"
  [job-story]
  (insert-row! :JobStory job-story)
  (get-last-insert-id))

(defn add-ethlance-job-story
  "Inserts a EthlanceJobStory. Returns autoincrement id"
  [ethlance-job-story]
  (insert-row! :JobStory ethlance-job-story)
  (let [job-story-id (get-last-insert-id)]
    (insert-row! :EthlanceJobStory (assoc ethlance-job-story
                                          :job-story/id job-story-id))))

(defn add-job-story-message [job-story-message]
  (insert-row! :JobStoryMessage job-story-message))

(defn add-message-file [message-id {:keys [:file/name :file/hash :file/directory-hash] :as file}]
  (let [file-id (do
                  (insert-row! :File file)
                  (get-last-insert-id))]
    (insert-row! :MessageFile {:message/id message-id
                               :file/id file-id})))

(defn update-ethlance-job-candidate [ethlance-job-id user-address]
  (update-row! :EthlanceJob {:ethlance-job/id ethlance-job-id
                             :ethlance-job/candidate user-address}))

(defn get-job-story-id-by-standard-bounty-id [bounty-id]
  (:id (db/get {:select [[:js.job-story/id :id]]
                :from [[:JobStory :js]]
                :join [[:Job :j] [:= :js.job/id :j.job/id]
                       [:StandardBounty :sb] [:= :j.job/id :sb.job/id]]
                :where [:= :sb.standard-bounty/id bounty-id]})))

(defn set-job-story-invoice-status-for-bounties [bounty-id invoice-ref-id status]
  (let [job-story-id (get-job-story-id-by-standard-bounty-id bounty-id)]
    (db/run! {:update :JobStoryInvoiceMessage
              :set {:invoice/status status}
              :where [:and
                      [:= :job-story/id job-story-id]
                      [:= :invoice/ref-id invoice-ref-id]]})))

(defn get-job-story-id-by-ethlance-job-id [ethlance-job-id]
  (:id (db/get {:select [[:js.job-story/id :id]]
                :from [[:JobStory :js]]
                :join [[:Job :j] [:= :js.job/id :j.job/id]
                       [:EthlanceJob :ej] [:= :j.job/id :ej.job/id]]
                :where [:= :ej.ethlance-job/id ethlance-job-id]})))

(defn set-job-story-invoice-status-for-ethlance-job [ethlance-job-id invoice-id status]
  (let [job-story-id (get-job-story-id-by-ethlance-job-id ethlance-job-id)]
    (db/run! {:update :JobStoryInvoiceMessage
              :set {:invoice/status status}
              :where [:and
                      [:= :job-story/id job-story-id]
                      [:= :invoice/ref-id invoice-id]]})))

(defn add-job-arbiter [job-id user-address]
  (insert-row! :JobArbiter {:job/id job-id
                            :user/address user-address}))

(defn add-contribution [job-id contributor-address contribution-id amount]
  (insert-row! :JobContribution {:job/id job-id
                                 :user/address contributor-address
                                 :job-contribution/amount amount
                                 :job-contribution/id contribution-id}))

(defn refund-job-contribution [job-id contribution-id]
  ;; TODO: implement this, delete from the table
  )

(defn update-job-approvers [job-id approvers-addresses]
  ;; TODO: implement this
  ;; we can grab the current ones, remove the ones that shouldn't be there
  ;; delete all from db and insert the calculated ones again
  ;; NOTE: the problem with implementing this is we don't have fee and fee-currency-id for the new ones
  )

(defn start
  "Start the ethlance-db mount component."
  [{:keys [:resync?] :as opts}]
  (log/info "Starting Ethlance DB component" {})
  (when resync?
    (log/info "Database module called with a resync flag.")
    (drop-db!))
  (create-db!)
  (log/info "Ethlance DB component started" {}))

(defn stop
  "Stop the ethlance-db mount component."
  []
  ::stopped)
