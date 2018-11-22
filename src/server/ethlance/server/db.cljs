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
  "

  ;;
  ;; User Tables
  ;;
  [{:table-name :User
    :table-columns
    [[:user/id :unsigned :integer primary-key]
     [:user/address address not-nil (sql/raw "UNIQUE")]
     [:user/country-code :varchar not-nil]
     [:user/email :varchar not-nil]
     [:user/profile-image :varchar]
     [:user/date-last-active :unsigned :integer]
     [:user/date-joined :unsigned :integer]]}

   ;; TODO: user_id foreign key
   {:table-name :UserCandidate
    :table-columns
    [[:user/id :unsigned :integer]
     [:candidate/biography :varchar]
     [:candidate/date-registered :unsigned :integer not-nil]
     [:candidate/profession-title :varchar not-nil]]}

   {:table-name :UserCandidateCategory
    :table-columns
    [[:user/id :unsigned :integer]
     [:candidate/category :varchar]]}

   {:table-name :UserCandidateSkill
    :table-columns
    [[:user/id :unsigned :integer]
     [:candidate/skill :varchar]]}

   ;; TODO: uid foreign key
   {:table-name :UserEmployer
    :table-columns
    [[:user/id :unsigned :integer]
     [:employer/biography :varchar]
     [:employer/date-registered :unsigned :integer not-nil]
     [:employer/profession-title :varchar not-nil]]}

   ;; TODO: uid foreign key
   {:table-name :UserArbiter
    :table-columns
    [[:user/id :unsigned :integer]
     [:arbiter/biography :varchar]
     [:arbiter/date-registered :unsigned :integer not-nil]
     [:arbiter/currency-type :unsigned :integer not-nil]
     [:arbiter/payment-value :BIG :INT not-nil]
     [:arbiter/payment-type :unsigned :integer not-nil]]}

   ;; TODO: uid foreign key
   {:table-name :UserGithub
    :table-columns
    [[:user/id :unsigned :integer]
     [:user-github/api-key :varchar not-nil]]}

   ;; TODO uid foreign key
   {:table-name :UserLinkedin
    :table-columns
    [[:user/id :unsigned :integer]
     [:user-linkedin/api-key :varchar not-nil]]}

   ;; TODO uid foreign key
   {:table-name :UserLanguage
    :table-columns
    [[:user/id :unsigned :integer]
     [:user/language :varchar not-nil]]}

   ;;
   ;; Job Tables
   ;;
   {:table-name :Job
    :table-columns
    [[:job/id :unsigned :integer primary-key]
     [:job/title :varchar not-nil]
     [:job/accepted-arbiter address default-nil]
     [:job/availability :unsigned :integer not-nil]
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
     [:job/reward-value :BIG :INT default-zero]]}

   ;; TODO jid foreign key
   {:table-name :JobArbiterRequest
    :table-columns
    [[:job/id :unsigned :integer]
     [:arbiter-request/arbiter-uid address not-nil]
     [:arbiter-request/is-employer-request? :unsigned :integer not-nil]]}

   ;; TODO jid foreign key
   {:table-name :JobSkills
    :table-columns
    [[:job/id :unsigned :integer]
     [:job/skill :varchar not-nil]]}

   ;;
   ;; Work Contract
   ;;

   ;; TODO jid foreign key
   {:table-name :WorkContract
    :table-columns
    [[:work-contract/id :unsigned :integer primary-key]
     [:job/id :unsigned :integer]
     [:work-contract/contract-status :unsigned :integer not-nil]
     [:work-contract/date-updated :unsigned :integer not-nil]
     [:work-contract/date-created :unsigned :integer not-nil]
     [:work-contract/date-finished :unsigned :integer default-zero]]}

   ;; TODO wid foreign key
   {:table-name :WorkContractInvoice
    :table-columns
    [[:invoice/id :unsigned :integer primary-key]
     [:work-contract/id :unsigned :integer]
     [:invoice/date-created :unsigned :integer not-nil]
     [:invoice/date-updated :unsigned :integer not-nil]
     [:invoice/date-paid :unsigned :integer default-zero]
     [:invoice/amount-requested :BIG :INT default-zero]
     [:invoice/amount-paid :BIT :INT default-nil]]}

   ;; TODO wid foreign key, uid foreign key
   {:table-name :WorkContractInvoiceComment
    :table-columns
    [[:comment/id :unsigned :integer primary-key]
     [:invoice/id :unsigned :integer]
     [:user/id :unsigned :integer]
     [:comment/user-type :unsigned :integer]
     [:comment/date-created :unsigned :integer not-nil]
     [:comment/data :varchar not-nil]]}

   ;; TODO wid foreign key
   {:table-name :WorkContractDispute
    :table-columns
    [[:dispute/id :unsigned :integer primary-key]
     [:work-contract/id :unsigned :integer]
     [:dispute/reason :varchar not-nil]
     [:dispute/date-created :unsigned :integer not-nil]
     [:dispute/date-updated :unsigned :integer not-nil]
     [:dispute/date-resolved :unsigned :integer default-nil]]}

   ;; TODO wid foreign key, uid foreign key
   {:table-name :WorkContractDisputeComment
    :table-columns
    [[:comment/id :unsigned :integer primary-key]
     [:dispute/id :unsigned :integer]
     [:user/id :unsigned :integer]
     [:comment/user-type :unsigned :integer]
     [:comment/date-created :unsigned :integer not-nil]
     [:comment/data :varchar not-nil]]}])


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


(defn create-db!
  "Creates the database with tables defined in the `database-schema`."
  []
  (log/info "Creating in-memory database...")
  (doseq [{:keys [table-name table-columns]} database-schema]
    (log/debug (str/format "  - Creating Database Table '%s' ..." table-name))
    (db/run! {:create-table [table-name] :with-columns [table-columns]}))
  (log/debug "Tables Created: " (list-tables)))


(defn drop-db!
  "Drops all of the database tables defined in the `database-schema`."
  []
  (log/info "Dropping in-memory database...")
  (doseq [{:keys [table-name]} (reverse database-schema)]
    (log/debug (str/format "  - Dropping Database Table '%s' ..." table-name))
    (db/run! {:drop-table [table-name]})))


(defn insert-row!
  "Inserts into the given `table-name` with the given `item`. The
  table-name and item structure are defined in the `database-schema`."
  [table-name item]
  (if-let [table-schema (get-table-schema table-name)]
    (let [item (select-keys item (:table-columns table-schema))]
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
     (let [item (select-keys item (:table-columns table-schema))]
       (db/run! {:update table-name
                 :set item
                 :where (concat
                         [:and]
                         (for [id-key (:id-keys table-schema)]
                           [:= id-key (get item id-key)]))})))))


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
     (let [item (select-keys item (:table-columns table-schema))]
       (db/run! {:select fields
                 :from [table-name]
                 :where (concat
                         [:and]
                         (for [id-key (:id-keys table-schema)]
                           [:= id-key (get item id-key)]))})))))


(defn start
  "Start the ethlance-db mount component."
  [config]
  (create-db!))


(defn stop
  "Stop the ethlance-db mount component."
  []
  (drop-db!))
