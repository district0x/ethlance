(ns ethlance.server.db
  "Represents the ethlance in-memory sqlite database. Contains a mount
  component for creating the in-memory database upon initial load."
  (:require
   [cuerdas.core :as str]
   [com.rpl.specter :as $ :include-macros true]
   [district.server.config :refer [config]]
   [district.server.db :as db]
   [district.server.db.column-types :refer [address not-nil default-nil default-zero default-false sha3-hash primary-key]]
   [district.server.db.honeysql-extensions]
   [honeysql.core :as sql]
   [honeysql.helpers :refer [merge-where merge-order-by merge-left-join defhelper]]
   [medley.core :as medley]
   [mount.core :as mount :refer [defstate]]
   [print.foo :refer [look] :include-macros true]
   [taoensso.timbre :as log]))


(declare start stop)


(def mount-state-key
  "Key defining our mount component within the district configuration"
  :ethlance/db)


(defstate ^{:on-reload :noop} ethlance-db
  :start (start (merge (get @config mount-state-key)
                       (get (mount/args) mount-state-key)))
  :stop (stop))


(def database-schema
  "Represents the database schema, consisting of tables, and their
  column descriptions.

  Notes:

  - Table Entry order matters for creation and deletion.
  
  - :id-keys is a listing which makes up a table's compound key

  - primary key auto-increments start from index 1.
  "

  ;;
  ;; User Tables
  ;;
  [{:table-name :User
    :table-columns
    [[:user/id :integer primary-key]
     [:user/address address not-nil]
     [:user/country-code :varchar not-nil]
     [:user/email :varchar not-nil]
     [:user/profile-image :varchar]
     [:user/date-last-active :unsigned :integer]
     [:user/date-joined :unsigned :integer]]
    :id-keys [:user/id]}

   ;; TODO: user_id foreign key
   {:table-name :UserCandidate
    :table-columns
    [[:user/id :integer primary-key]
     [:candidate/biography :varchar]
     [:candidate/date-registered :unsigned :integer not-nil]
     [:candidate/professional-title :varchar not-nil]]
    :id-keys [:user/id]}

   {:table-name :UserCandidateCategory
    :table-columns
    [[:user/id :integer]
     [:candidate/category :varchar]
     [(sql/call :primary-key :user/id)]]
    :id-keys [:user/id]}

   {:table-name :UserCandidateSkill
    :table-columns
    [[:user/id :integer]
     [:candidate/skill :varchar]]
    :id-keys [:user/id]}

   ;; TODO: uid foreign key
   {:table-name :UserEmployer
    :table-columns
    [[:user/id :integer primary-key]
     [:employer/biography :varchar]
     [:employer/date-registered :unsigned :integer not-nil]
     [:employer/professional-title :varchar not-nil]]
    :id-keys [:user/id]}

   ;; TODO: uid foreign key
   {:table-name :UserArbiter
    :table-columns
    [[:user/id :integer primary-key]
     [:arbiter/biography :varchar]
     [:arbiter/date-registered :unsigned :integer not-nil]
     [:arbiter/currency-type :unsigned :integer not-nil]
     [:arbiter/payment-value :BIG :INT not-nil]
     [:arbiter/payment-type :unsigned :integer not-nil]]
    :id-keys [:user/id]}

   ;; TODO: Consider normalizing, foreign key
   {:table-name :UserGithub
    :table-columns
    [[:user/id :integer primary-key]
     [:github/api-key :varchar not-nil]]
    :id-keys [:user/id]}

   ;; TODO Consider normalizing, foreign key
   {:table-name :UserLinkedin
    :table-columns
    [[:user/id :integer primary-key]
     [:linkedin/api-key :varchar not-nil]]
    :id-keys [:user/id]}

   ;; TODO Foreign key
   {:table-name :UserLanguage
    :table-columns
    [[:user/id :integer]
     [:user/language :varchar not-nil]]
    :id-keys [:user/id]}

   ;;
   ;; Job Tables
   ;;
   {:table-name :Job
    :table-columns
    [[:job/id :integer primary-key]
     [:job/title :varchar not-nil]
     [:job/accepted-arbiter address default-nil]
     [:job/availability :integer not-nil]
     [:job/bid-option :unsigned :integer not-nil]
     [:job/category :varchar not-nil]
     [:job/description :varchar default-nil]
     [:job/date-created :unsigned :integer not-nil]
     [:job/date-started :unsigned :integer default-nil]
     [:job/date-finished :unsigned :intreger default-nil]
     [:job/employer-uid address not-nil]
     [:job/estimated-length-seconds :unsigned :integer default-zero]
     [:job/include-ether-token? :unsigned :integer not-nil]
     [:job/is-invitation-only? :unsigned :integer not-nil]
     [:job/reward-value :BIG :INT default-zero]]
    :id-keys [:job/id]}

   ;; TODO(?) user TRIGGER for UserArbiter existence.
   {:table-name :JobArbiterRequest
    :table-columns
    [[:job/id :integer]
     [:user/id address not-nil] ;; requesting arbiter
     [:arbiter-request/date-requested :unsigned :integer]
     [:arbiter-request/is-employer-request? :unsigned :integer not-nil]
     [(sql/call :primary-key :job/id :user/id)]]
    :id-keys [:job/id :user/id]}

   ;; TODO job/id foreign key
   {:table-name :JobSkills
    :table-columns
    [[:job/id :integer]
     [:job/skill :varchar not-nil]]
    :id-keys [:job/id]}

   ;;
   ;; Work Contract
   ;;
   ;; Notes:
   ;;
   ;; - JobStore holds a listing of WorkContract, hence the compound key
   ;;
   ;; - WorkContract holds listings of invoices and disputes. Given
   ;;   that WorkContract is compounded with JobStore, it requires a
   ;;   triple compound key.

   ;; TODO foreign key
   {:table-name :WorkContract
    :table-columns
    [[:job/id :integer]
     [:work-contract/index :integer]
     [:work-contract/contract-status :unsigned :integer not-nil]
     [:work-contract/date-updated :unsigned :integer not-nil]
     [:work-contract/date-created :unsigned :integer not-nil]
     [:work-contract/date-finished :unsigned :integer default-zero]
     [(sql/call :primary-key :job/id :work-contract/index)]]
    :id-keys [:job/id :work-contract/index]}

   {:table-name :WorkContractInvoice
    :table-columns
    [[:job/id :integer]
     [:work-contract/index :integer]
     [:invoice/index :integer]
     [:invoice/date-created :unsigned :integer not-nil]
     [:invoice/date-updated :unsigned :integer not-nil]
     [:invoice/date-paid :unsigned :integer default-zero]
     [:invoice/amount-requested :BIG :INT default-zero]
     [:invoice/amount-paid :BIT :INT default-nil]
     [(sql/call :primary-key :job/id :work-contract/index :invoice/index)]]
    :id-keys [:job/id :work-contract/id :invoice/index]}

   ;; TODO wid foreign key, uid foreign key
   {:table-name :WorkContractInvoiceComment
    :table-columns
    [[:job/id :integer]
     [:work-contract/index :integer]
     [:invoice/index :integer]
     [:comment/id :integer]
     [:user/id :unsigned :integer] ;; User ID of Arbiter, Candidate, or Employer.
     [:comment/user-type :unsigned :integer]
     [:comment/date-created :unsigned :integer not-nil]
     [:comment/data :varchar not-nil]
     [(sql/call :primary-key :job/id :work-contract/index :invoice/index :comment/id)]] 
    :id-keys [:job/id :work-contract/index :invoice/index :comment/id]}

   ;; TODO wid foreign key
   {:table-name :WorkContractDispute
    :table-columns
    [[:job/id :integer]
     [:work-contract/index :integer]
     [:dispute/index :integer]
     [:dispute/reason :varchar not-nil]
     [:dispute/date-created :unsigned :integer not-nil]
     [:dispute/date-updated :unsigned :integer not-nil]
     [:dispute/date-resolved :unsigned :integer default-nil]
     [(sql/call :primary-key :job/id :work-contract/index :dispute/index)]]
    :id-keys [:job/id :work-contract/index :dispute/index]}

   ;; TODO wid foreign key, uid foreign key
   {:table-name :WorkContractDisputeComment
    :table-columns
    [[:job/id :integer]
     [:work-contract/index :integer]
     [:dispute/index :integer]
     [:comment/id :integer]
     [:user/id :unsigned :integer]
     [:comment/user-type :unsigned :integer]
     [:comment/date-created :unsigned :integer not-nil]
     [:comment/data :varchar not-nil]
     [(sql/call :primary-key :job/id :work-contract/index :dispute/index :comment/id)]]
    :id-keys [:job/id :work-contract/index :dispute/index :comment/id]}])


(defn list-tables
  "Lists all of the tables currently in the sqlite3 database."
  []
  (db/all {:select [:name]
           :from [:sqlite_master]
           :where [:= :type "table"]}))


(defn- get-table-schema
  "Retrieve the given table schema defined by `table-name` from the
  database schema, or nil."
  [table-name]
  (-> ($/select [$/ALL #(= (:table-name %) table-name)] database-schema)
      first))


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
    (db/run! {:create-table [table-name] :with-columns [table-columns]}))
  (log/debug "Tables Created: " (list-tables)))


(defn drop-db!
  "Drops all of the database tables defined in the `database-schema`."
  []
  (log/info "Dropping Sqlite Database...")
  (doseq [{:keys [table-name]} (reverse database-schema)]
    (log/debug (str/format "  - Dropping Database Table '%s' ..." table-name))
    (db/run! {:drop-table [table-name]})))


(defn insert-row!
  "Inserts into the given `table-name` with the given `item`. The
  table-name and item structure are defined in the `database-schema`."
  [table-name item]
  (if-let [table-schema (get-table-schema table-name)]
    (let [table-column-names (get-table-column-names table-name)
          item (select-keys item table-column-names)]
      (log/debug "Item: " item)
      (log/debug "Table Schema: " table-schema)
      (db/run! {:insert-into table-name
                :columns (keys item)
                :values [(vals item)]}))
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
            item (select-keys item table-column-names)]
        (db/run! {:update table-name
                  :set item
                  :where (concat
                          [:and]
                          (for [id-key (:id-keys table-schema)]
                            [:= id-key (get item id-key)]))})))
    (log/error (str/format "Unable to find table schema for '%s'" table-name))))


(defn get-row
  "Get the given `fields` for the data row described by the `table-name`
  and `item` containing appropriate `item-keys` to identify the item.

  Notes:

  - The :id-keys within the table-schema need to be defined.
  "
  [table-name item & fields]
  (if-let [table-schema (get-table-schema table-name)]
    (do
      (assert (not (empty? (:id-keys table-schema)))
              (str/format ":id-keys for table schema '%s' is required for getting rows." table-name))
      (let [table-column-names (get-table-column-names table-name)
            item (select-keys item table-column-names)
            _ (log/debug "Item" item)
            fields (or fields table-column-names)]
        (db/get {:select fields
                 :from [table-name]
                 :where (concat
                         [:and]
                         (for [id-key (:id-keys table-schema)]
                           [:= id-key (get item id-key)]))})))
    (log/error (str/format "Unable to find table schema for '%s'" table-name))))


(defn start
  "Start the ethlance-db mount component."
  [config]
  (create-db!))


(defn stop
  "Stop the ethlance-db mount component."
  []
  (drop-db!))
