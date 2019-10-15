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

  - :list-keys is a listing which makes up a table's key for producing
  a proper listing.
  "
  ;;
  ;; Ethlance Tables
  ;;
  [])


(defn list-tables
  "Lists all of the tables currently in the sqlite3 database."
  []
  (let [results
        (db/all {:select [:name]
                 :from [:sqlite_master]
                 :where [:= :type "table"]})]
    (mapv :name results)))


(defn table-exists?
  [name]
  (contains? (set (list-tables)) name))


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
      (not-empty (db/run! {:insert-into table-name
                           :columns (keys item)
                           :values [(vals item)]})))
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
        (not-empty (db/run! {:update table-name
                             :set item
                             :where (concat
                                     [:and]
                                     (for [id-key (:id-keys table-schema)]
                                       [:= id-key (get item id-key)]))}))))
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


(defn start
  "Start the ethlance-db mount component."
  [config]
  (create-db!))


(defn stop
  "Stop the ethlance-db mount component."
  []
  (drop-db!))

