(ns ethlance.server.db
  "Represents the ethlance in-memory sqlite database. Contains a mount
  component for creating the in-memory database upon initial load."
  (:require [clojure.pprint :as pprint]
            [clojure.set :as set]
            [cljs.core.async :as async :refer [go-loop <!]]
            [com.rpl.specter :as $ :include-macros true]
            [cuerdas.core :as str]
            [district.server.async-db :as db]
            [district.server.config :refer [config]]
            [district.server.db.column-types :as column-types :refer [not-nil]]
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
  [{:table-name :Users
    :table-columns
    [[:user/id column-types/address]
     [:user/email :varchar not-nil]
     [:user/name :varchar not-nil]
     [:user/country :varchar] ; FIXME: Not asked or set currently at registration. Remove?
     [:user/profile-image :varchar]
     [:user/date-registered :bigint]
     [:user/date-updated :bigint]
     [:user/github-username :varchar]
     [:user/linkedin-username :varchar]
     [:user/status :varchar]
     ;; PK
     [(sql/call :primary-key :user/id)]]
    :list-keys []}

   {:table-name :UserSocialAccounts
    :table-columns
    [[:user/id column-types/address]
     [:user/github-username :varchar]
     [:user/linkedin-username :varchar]
     ;; PK
     [(sql/call :primary-key :user/id)]]
    :list-keys []}

   {:table-name :Candidate
    :table-columns
    [[:user/id column-types/address]
     [:candidate/bio :varchar]
     [:candidate/professional-title :varchar]
     [:candidate/rate :integer not-nil]
     [:candidate/rate-currency-id :varchar not-nil]
     [:candidate/rating :real]
     ;; PK
     [(sql/call :primary-key :user/id)]
     ;; FKs
     [(sql/call :foreign-key :user/id) (sql/call :references :Users :user/id) (sql/raw "ON DELETE CASCADE")]]
    :list-keys []}

   {:table-name :Employer
    :table-columns
    [[:user/id column-types/address]
     [:employer/bio :varchar]
     [:employer/professional-title :varchar]
     [:employer/rating :real]
     ;; PK
     [(sql/call :primary-key :user/id)]
     ;; FKs
     [(sql/call :foreign-key :user/id) (sql/call :references :Users :user/id) (sql/raw "ON DELETE CASCADE")]]
    :list-keys []}

   {:table-name :Arbiter
    :table-columns
    [[:user/id column-types/address]
     [:arbiter/bio :varchar]
     [:arbiter/professional-title :varchar]
     [:arbiter/fee :integer not-nil]
     [:arbiter/fee-currency-id :varchar not-nil]
     [:arbiter/rating :real]
     ;; PK
     [(sql/call :primary-key :user/id)]
     ;; FKs
     [(sql/call :foreign-key :user/id) (sql/call :references :Users :user/id) (sql/raw "ON DELETE CASCADE")]]
    :list-keys []}

   {:table-name :UserLanguage
    :table-columns
    [[:user/id column-types/address]
     [:language/id :varchar not-nil]
     ;; PK
     [(sql/call :primary-key :user/id :language/id)]
     ;; FKs
     [(sql/call :foreign-key :user/id) (sql/call :references :Users :user/id) (sql/raw "ON DELETE CASCADE")]]
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

   {:table-name :TokenDetail
    :table-columns
    [[:token-detail/id column-types/address]
     [:token-detail/name :text]
     [:token-detail/symbol :text]
     [:token-detail/abi :text]
     ;; PK
     [(sql/call :primary-key :token-detail/id)]
     ]
    :list-keys []}
   {:table-name :ArbiterCategory
    :table-columns
    [[:user/id column-types/address]
     [:category/id :varchar not-nil]
     ;; PK
     [(sql/call :primary-key :user/id :category/id)]
     ;; FKs
     [(sql/call :foreign-key :user/id) (sql/call :references :Users :user/id) (sql/raw "ON DELETE CASCADE")]
     [(sql/call :foreign-key :category/id) (sql/call :references :Category :category/id) (sql/raw "ON DELETE CASCADE")]]
    :list-keys []}

   {:table-name :ArbiterSkill
    :table-columns
    [[:user/id :varchar not-nil]
     [:skill/id :varchar not-nil]
     ;; PK
     [(sql/call :primary-key :user/id :skill/id)]
     ;; FKs
     [(sql/call :foreign-key :user/id) (sql/call :references :Users :user/id) (sql/raw "ON DELETE CASCADE")]
     [(sql/call :foreign-key :skill/id) (sql/call :references :Skill :skill/id) (sql/raw "ON DELETE CASCADE")]]
    :list-keys []}

   {:table-name :CandidateCategory
    :table-columns
    [[:user/id :varchar not-nil]
     [:category/id :varchar not-nil]
     ;; PK
     [(sql/call :primary-key :user/id :category/id)]
     ;; FKs
     [(sql/call :foreign-key :user/id) (sql/call :references :Users :user/id) (sql/raw "ON DELETE CASCADE")]
     [(sql/call :foreign-key :category/id) (sql/call :references :Category :category/id) (sql/raw "ON DELETE CASCADE")]]
    :list-keys []}

   {:table-name :CandidateSkill
    :table-columns
    [[:user/id :varchar not-nil]
     [:skill/id :varchar not-nil]
     ;; PK
     [(sql/call :primary-key :user/id :skill/id)]
     ;; FKs
     [(sql/call :foreign-key :user/id) (sql/call :references :Users :user/id) (sql/raw "ON DELETE CASCADE")]
     [(sql/call :foreign-key :skill/id) (sql/call :references :Skill :skill/id) (sql/raw "ON DELETE CASCADE")]]
    :list-keys []}

   {:table-name :Job
    :table-columns
    [[:job/id column-types/address] ; add unique & not null constraints
                                    ; https://github.com/seancorfield/honeysql/blob/develop/doc/clause-reference.md
                                    ; https://github.com/district0x/d0x-libs/blob/master/server/district-server-db/src/district/server/db/column_types.cljs
     [:job/creator column-types/address]
     [:job/title :varchar not-nil]
     [:job/description :varchar not-nil]
     [:job/category :varchar]
     [:job/status :varchar]
     [:job/date-created :bigint]
     [:job/date-updated :bigint]
     [:job/required-experience-level :text]

     ; new fields (remove previous 3 :job/{:token/token-version/reward})
     [:job/token-type :text]
     [:job/token-amount :bigint]
     [:job/token-address :text]
     [:job/token-id :integer]

     ; These fields had :ethlance-job/estimated-project-length prefix
     ; (originally from EthlanceJob table). Find places where to rename
     [:job/estimated-project-length :text]
     [:job/max-number-of-candidates :integer] ; TODO: remove
     [:job/invitation-only? :bool]
     [:job/required-availability :text]
     [:job/hire-address :varchar]
     [:job/bid-option :text]

     [:job/language-id :varchar] ; TODO: REMOVE
     ;; PK
     [(sql/call :primary-key :job/id)]]
    :list-keys []}

   {:table-name :JobCreator
    :table-columns
    [[:job/id column-types/address]
     [:user/id :varchar]
     ;; PK
     [(sql/call :primary-key :job/id :user/id)]
     ;; FKs
     [(sql/call :foreign-key :user/id) (sql/call :references :Users :user/id) (sql/raw "ON DELETE CASCADE")]
     [(sql/call :foreign-key :job/id) (sql/call :references :Job :job/id) (sql/raw "ON DELETE CASCADE")]]
    :list-keys []}

   {:table-name :JobContribution
    :table-columns
    [[:job/id column-types/address]
     [:user/id :varchar]
     [:job-contribution/amount :bigint]
     [:job-contribution/id :integer]

     ;; PK
     [(sql/call :primary-key :job/id :user/id)]

     ;; FKs
     [(sql/call :foreign-key :job/id) (sql/call :references :Job :job/id) (sql/raw "ON DELETE CASCADE")]
     ]}

   {:table-name :JobSkill
    :table-columns
    [[:job/id column-types/address]
     [:skill/id :varchar]
     ;; PK
     [(sql/call :primary-key :job/id :skill/id)]
     ;; FKs
     [(sql/call :foreign-key :job/id) (sql/call :references :Job :job/id) (sql/raw "ON DELETE CASCADE")]]
    :list-keys []}

   {:table-name :JobArbiter
    :table-columns
    [[:job/id column-types/address]
     [:user/id :varchar]
     [:job-arbiter/fee :integer]
     [:job-arbiter/fee-currency-id :varchar]
     [:job-arbiter/status :varchar]
     [:job-arbiter/date-accepted :bigint]
     ;; PK
     [(sql/call :primary-key :job/id :user/id)]

     ;; FKs
     [(sql/call :foreign-key :user/id) (sql/call :references :Users :user/id) (sql/raw "ON DELETE CASCADE")]
     [(sql/call :foreign-key :job/id) (sql/call :references :Job :job/id) (sql/raw "ON DELETE CASCADE")]]
    :list-keys []}

   {:table-name :JobFile
    :table-columns
    [[:job/id column-types/address]
     [:job/file-id :integer]

     ;; FKs
     [(sql/call :foreign-key :job/id) (sql/call :references :Job :job/id) (sql/raw "ON DELETE CASCADE")]
     ]
    :list-keys []}

   {:table-name :Message
    :table-columns
    [[:message/id :serial]
     [:message/creator :varchar]
     [:message/text :varchar]
     [:message/date-created :bigint]
     ;; proposal, invitation, raised dispute, resolved dispute, feedback, invoice, direct message, job story message
     [:message/type :varchar not-nil]
     ;; PK
     [(sql/call :primary-key :message/id)]
     ;; FKs
     [(sql/call :foreign-key :message/id) (sql/call :references :Message :message/id) (sql/raw "ON DELETE CASCADE")]]
    :list-keys []}

   {:table-name :JobStory
    :table-columns
    [[:job-story/id :serial]
     [:job/id column-types/address]
     [:job-story/status :varchar]
     [:job-story/date-created :bigint]
     [:job-story/date-updated :bigint]
     [:job-story/invitation-message-id :integer]
     [:job-story/proposal-message-id :integer]
     [:job-story/raised-dispute-message-id :integer]
     [:job-story/resolved-dispute-message-id :integer]
     [:job-story/proposal-rate :integer]
     [:job-story/proposal-rate-currency-id :varchar]

     ; The following used to be :ethlance-job-story/...
     [:job-story/candidate :varchar]
     [:job-story/date-candidate-accepted :bigint]

     ;; PK
     [(sql/call :primary-key :job-story/id)]

     ;; FKs
     [(sql/call :foreign-key :job/id) (sql/call :references :Job :job/id) (sql/raw "ON DELETE CASCADE")]
     [(sql/call :foreign-key :job-story/invitation-message-id) (sql/call :references :Message :message/id) (sql/raw "ON DELETE CASCADE")]
     [(sql/call :foreign-key :job-story/proposal-message-id) (sql/call :references :Message :message/id) (sql/raw "ON DELETE CASCADE")]]
    :list-keys []}

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
     [:invoice/amount-requested :bigint]
     [:invoice/amount-paid :bigint]
     [:invoice/date-paid :bigint]
     [:invoice/date-work-started :bigint]
     [:invoice/date-work-ended :bigint]
     [:invoice/work-duration :bigint]
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
     [:user/id :varchar not-nil]

     ;; PK
     [(sql/call :primary-key :job-story/id :message/id)]
     ;; FKs
     [(sql/call :foreign-key :job-story/id) (sql/call :references :JobStory :job-story/id) (sql/raw "ON DELETE CASCADE")]
     [(sql/call :foreign-key :user/id) (sql/call :references :Users :user/id) (sql/raw "ON DELETE CASCADE")]
     [(sql/call :foreign-key :message/id) (sql/call :references :Message :message/id) (sql/raw "ON DELETE CASCADE")]]
    :list-keys []}


   {:table-name :DirectMessage
    :table-columns
    [[:message/id :integer]
     [:direct-message/receiver :varchar]
     [:direct-message/read? :integer]
     ;; PK
     [(sql/call :primary-key :message/id)]
     ;; FKs
     [(sql/call :foreign-key :message/id) (sql/call :references :Message :message/id) (sql/raw "ON DELETE CASCADE")]]

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

   {:table-name :ReplayEventQueue
    :table-columns
    [[:event/comparable-id :integer]
     [:event/string :varchar]

     [(sql/call :primary-key :event/comparable-id)]]}])

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

(defn- get-table-columns-by-type [table-name type-pred]
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

(defn filter-tables [table-names schema]
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Row manipulation utils ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def auto-increment-types #{:serial})

(defn insert-row!
  "Inserts into the given `table-name` with the given `item`. The
  table-name and item structure are defined in the `database-schema`."
  [conn table-name item]
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
                      :returning [:*]}]
       (not-empty (try
                    (first (<? (db/run! conn statement)))
                    (catch js/Error e
                      (log/error "Error executing insert statement" {:error e
                                                                     :statement statement})))))
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Application level db access ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
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

(defn upsert-user! [conn {:user/keys [type] :as user}]
  (safe-go
   (let [values (select-keys user (get-table-column-names :Users))
         _ (<? (db/run! conn
                        {:insert-into :Users,
                         :values [values]
                         :upsert
                         (array-map :on-conflict [:user/id]
                                    :do-update-set (keys values))}))]
     (case type
       :arbiter (let [arbiter (select-keys user (get-table-column-names :Arbiter))]
                  (<? (db/run! conn {:insert-into :Arbiter
                                     :values [arbiter]
                                     :upsert (array-map :on-conflict [:user/id]
                                                        :do-update-set (keys arbiter))})))
       :employer (let [employer (select-keys user (get-table-column-names :Employer))]
                   (<? (db/run! conn {:insert-into :Employer
                                      :values [employer]
                                      :upsert (array-map :on-conflict [:user/id]
                                                         :do-update-set (keys employer))})))
       :candidate (let [candidate (select-keys user (get-table-column-names :Candidate))]
                    (<? (db/run! conn {:insert-into :Candidate
                                       :values [candidate]
                                       :upsert (array-map :on-conflict [:user/id]
                                                          :do-update-set (keys candidate))}))
                    (doseq [address [(:user/id user)]
                            target [[:Category :CandidateCategory :category/id (:candidate/categories user)]
                                    [:Skill :CandidateSkill :skill/id (:candidate/skills user)]
                                    [nil :UserLanguage :language/id (:user/languages user)]]]
                      (let [[pk-table table column values] target]
                        (if-not (nil? pk-table) (<? (db/run! conn (add-missing-values pk-table values))))
                        (<? (db/run! conn (remove-old-associations address table)))
                        (<? (db/run! conn (add-new-associations address table column values))))))))))

(defn upsert-user-social-accounts! [conn user-social-accounts]
  (safe-go
    (let [values (select-keys user-social-accounts (get-table-column-names :UserSocialAccounts))]
      (<? (db/run! conn
                   {:insert-into :UserSocialAccounts,
                    :values [values]
                    :upsert
                    (array-map :on-conflict [:user/id]
                               :do-update-set (keys values))})))))

(defn add-skills [conn job-id skills]
  (safe-go
    (doseq [skill skills]
      (<? (insert-row! conn :JobSkill {:job/id job-id :skill/id skill})))))

(defn add-job [conn job]
  (safe-go
    (let [skills (:job/required-skills job)
          job-fields (dissoc job :job/required-skills)
          job-id (:job/id (<? (insert-row! conn :Job job)))]
      (add-skills conn job-id skills))))

; TODO: remove because 1) jobs are addressed via creator address or job contract adddress
;                      2) EthlanceJob doesn't exist (merged with Job after removing bounties)
(defn get-job-id-for-ethlance-job [conn ethlance-job-id]
  (safe-go ethlance-job-id))

(defn update-ethlance-job [conn job-id job-data]
  (safe-go
  (<? (update-row! conn :Job (assoc job-data :job/id job-id)))))

(defn update-job-story-invoice-message  [conn msg]
  (safe-go
   (<? (update-row! conn :JobStoryInvoiceMessage msg))))

(defn add-message
  "Inserts a Message. Returns autoincrement id"
  [conn message]
  (safe-go
   (let [msg-id (-> (<? (insert-row! conn :Message message))
                    :message/id)
         job-story-id (or (:job-story/id message)
                          (:job-story/id (<? (insert-row! conn :JobStory
                                                          {:job/id (:job/id message)
                                                           :job-story/candidate (:message/creator message)
                                                           :job-story/date-created (:message/date-created message)
                                                           :job-story/status "proposed"
                                                           :job-story/proposal-message-id msg-id
                                                           :job-story/proposal-rate (:job-story/proposal-rate message)}))))
         message (assoc message :message/id msg-id :job-story/id job-story-id)]
     (case (:message/type message)
       :job-story-message
       (do
         (<? (insert-row! conn :JobStoryMessage (assoc message
                                                       :message/id msg-id
                                                       :job-story/id job-story-id)))
         (<? (case (:job-story-message/type message)
               :raise-dispute (update-row! conn :JobStory (assoc message
                                                                 :job-story/id (:job-story/id message)
                                                                 :job-story/raised-dispute-message-id msg-id))
               :resolve-dispute (update-row! conn :JobStory (assoc message
                                                                   :job-story/id (:job-story/id message)
                                                                   :job-story/resolved-dispute-message-id msg-id))
               :proposal (update-row! conn :JobStory (assoc message
                                                            :job-story/id (:job-story/id message)
                                                            :job-story/proposal-message-id msg-id))
               :invitation (update-row! conn :JobStory (assoc message
                                                              :job-story/id (:job-story/id message)
                                                              :job-story/invitation-message-id msg-id))
               :invoice (insert-row! conn :JobStoryInvoiceMessage message)
               :feedback  (insert-row! conn :JobStoryFeedbackMessage message))))

       :direct-message
       (<? (insert-row! conn :DirectMessage message))))))

(defn add-job-story
  "Inserts a JobStory. Returns autoincrement id"
  [conn job-story]
  (safe-go
   (:job-story/id (<? (insert-row! conn :JobStory job-story)))))

(defn add-job-story-message [conn job-story-message]
  (safe-go
   (<? (insert-row! conn :JobStoryMessage job-story-message))))

(defn add-message-file [conn message-id file]
  (safe-go
   (let [{:file/keys [id]} (<? (insert-row! conn :File file))]
     (<? (insert-row! conn :MessageFile {:message/id message-id
                                         :file/id id})))))

(defn update-job-candidate [conn job-id user-address]
  (safe-go
   (<? (update-row! conn :EthlanceJob {:ethlance-job/id job-id
                                       :ethlance-job/candidate user-address}))))

(defn get-job-story-id-by-job-id [conn job-id]
  (safe-go
   (:id (<? (db/get conn {:select [[:js.job-story/id :id]]
                          :from [[:JobStory :js]]
                          :join [[:Job :j] [:= :js.job/id :j.job/id]]
                          :where [:= :j.job/id job-id]})))))

(defn set-job-story-invoice-status-for-job [conn job-id invoice-id status]
  (safe-go
   (let [job-story-id (<? (get-job-story-id-by-job-id conn job-id))]
     (<? (db/run! conn {:update :JobStoryInvoiceMessage
                        :set {:invoice/status status}
                        :where [:and
                                [:= :job-story/id job-story-id]
                                [:= :invoice/ref-id invoice-id]]})))))

(defn add-job-arbiter [conn job-id user-address]
  (safe-go
   (<? (insert-row! conn :JobArbiter {:job/id job-id
                                      :user/id user-address}))))

(defn add-contribution [conn job-id contributor-address contribution-id amount]
  (safe-go
   (<? (insert-row! conn :JobContribution {:job/id job-id
                                           :user/id contributor-address
                                           :job-contribution/amount amount
                                           :job-contribution/id contribution-id}))))

(defn refund-job-contribution [_ _ _]
  ;; [conn job-id contribution-id]
  ;; TODO: implement this, delete from the table
  (safe-go)
  )

(defn update-job-approvers [_ _ _]
  ;; [conn job-id approvers-addresses]
  ;; TODO: implement this
  ;; we can grab the current ones, remove the ones that shouldn't be there
  ;; delete all from db and insert the calculated ones again
  ;; NOTE: the problem with implementing this is we don't have fee and fee-currency-id for the new ones
  (safe-go)
  )

(defn get-token [conn token-address]
  (safe-go
    (<? (db/get conn {:select [:*]
                          :from [:TokenDetail]
                          :where [:= :TokenDetail.token-detail/id token-address]}))))

(defn store-token-details [conn token-details]
  (safe-go
    (<? (insert-row! conn :TokenDetail
                     {:token-detail/id (:address token-details)
                      :token-detail/name (:name token-details)
                      :token-detail/symbol (:symbol token-details)
                      :token-detail/abi (:abi-string token-details)}))))

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
