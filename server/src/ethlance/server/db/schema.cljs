(ns ethlance.server.db.schema
  (:require
    [district.server.db.column-types :as column-types :refer [not-nil]]
    [honeysql.core :as sql]))

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
     [(sql/call :foreign-key :user/id) (sql/call :references :Users :user/id) (sql/raw "ON DELETE CASCADE ON UPDATE CASCADE")]]
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
     [(sql/call :foreign-key :user/id) (sql/call :references :Users :user/id) (sql/raw "ON DELETE CASCADE ON UPDATE CASCADE")]]
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
     [(sql/call :foreign-key :user/id) (sql/call :references :Users :user/id) (sql/raw "ON DELETE CASCADE ON UPDATE CASCADE")]]
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
     [:token-detail/type :text not-nil] ; #{:eth :erc20 :erc721 :erc1155}
     [:token-detail/name :text]
     [:token-detail/symbol :text]
     [:token-detail/decimals :integer not-nil]
     ;; PK
     [(sql/call :primary-key :token-detail/id)]]
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
     ;; https://github.com/seancorfield/honeysql/blob/develop/doc/clause-reference.md
     ;; https://github.com/district0x/d0x-libs/blob/master/server/district-server-db/src/district/server/db/column_types.cljs
     [:job/creator column-types/address]
     [:job/title :varchar not-nil]
     [:job/description :varchar not-nil]
     [:job/category :varchar]
     [:job/status :varchar]
     [:job/date-created :bigint]
     [:job/date-updated :bigint]
     [:job/required-experience-level :text]

     ;; new fields (remove previous 3 :job/{:token/token-version/reward})
     [:job/token-type :text]
     [:job/token-amount :numeric]
     [:job/token-address :text]
     [:job/token-id :integer]

     ;; These fields had :ethlance-job/estimated-project-length prefix
     ;; (originally from EthlanceJob table). Find places where to rename
     [:job/estimated-project-length :text]
     [:job/invitation-only? :bool]
     [:job/required-availability :text]
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
     [(sql/call :foreign-key :job/id) (sql/call :references :Job :job/id) (sql/raw "ON DELETE CASCADE")]]}

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
     [:job-arbiter/fee :bigint]
     [:job-arbiter/fee-currency-id :varchar]
     [:job-arbiter/status :varchar]
     [:job-arbiter/date-created :bigint]
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
     [(sql/call :foreign-key :job/id) (sql/call :references :Job :job/id) (sql/raw "ON DELETE CASCADE")]]
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
     [:job-story/proposal-rate (sql/call :numeric (sql/inline 81) (sql/inline 3))] ; To cover the max value of Solidity's int256 (e.g. amount in ERC20) & support 3 places of precision
     [:job-story/proposal-rate-currency-id :varchar]

     ;; The following used to be :ethlance-job-story/...
     [:job-story/candidate :varchar]
     [:job-story/date-contract-active :bigint]

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
     [:job-story-message/type :text]
     ;; PK
     [(sql/call :primary-key :job-story/id :message/id)]
     ;; FKs
     [(sql/call :foreign-key :job-story/id) (sql/call :references :JobStory :job-story/id) (sql/raw "ON DELETE CASCADE")]
     [(sql/call :foreign-key :message/id) (sql/call :references :Message :message/id) (sql/raw "ON DELETE CASCADE")]]
    :list-keys []}

   {:table-name :JobStoryInvoiceMessage
    :table-columns
    [[:job-story/id :integer]
     [:message/id :integer]
     [:invoice/status :varchar]
     [:invoice/hours-worked :integer]
     [:invoice/hourly-rate :integer]
     [:invoice/amount-requested :bigint]
     [:invoice/amount-paid :bigint]
     [:invoice/date-requested :bigint]
     [:invoice/date-paid :bigint]
     [:invoice/ref-id :integer]
     [:invoice/payment-message-id :integer]
     [:invoice/dispute-raised-message-id :integer]
     [:invoice/dispute-resolved-message-id :integer]
     ;; PK
     [(sql/call :primary-key :job-story/id :message/id)]
     ;; FKs
     [(sql/call :foreign-key :job-story/id) (sql/call :references :JobStory :job-story/id) (sql/raw "ON DELETE CASCADE")]
     [(sql/call :foreign-key :message/id) (sql/call :references :Message :message/id) (sql/raw "ON DELETE CASCADE")]
     [(sql/call :foreign-key :invoice/payment-message-id) (sql/call :references :Message :message/id) (sql/raw "ON DELETE CASCADE")]
     [(sql/call :foreign-key :invoice/dispute-raised-message-id) (sql/call :references :Message :message/id) (sql/raw "ON DELETE CASCADE")]
     [(sql/call :foreign-key :invoice/dispute-resolved-message-id) (sql/call :references :Message :message/id) (sql/raw "ON DELETE CASCADE")]]
    :list-keys []}

   {:table-name :JobStoryFeedbackMessage
    :table-columns
    [[:job-story/id :integer not-nil]
     [:message/id :integer not-nil]
     [:feedback/rating :integer not-nil]
     [:feedback/receiver-role :varchar]
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
     [:direct-message/recipient :varchar]
     [:direct-message/read? :integer]
     [:job-story/id :integer]
     ;; PK
     [(sql/call :primary-key :message/id)]
     ;; FKs
     [(sql/call :foreign-key :message/id) (sql/call :references :Message :message/id) (sql/raw "ON DELETE CASCADE")]
     [(sql/call :foreign-key :job-story/id) (sql/call :references :JobStory :job-story/id) (sql/raw "ON DELETE CASCADE")]]

    :list-keys []}

   {:table-name :JobFunding
    :table-columns
    [[:tx column-types/sha3-hash]
     [:job/id column-types/address not-nil]
     [:job-funding/amount :numeric not-nil]
     [:job-funding/created-at :bigint]
     [:token-detail/id column-types/address]
     ;; PK
     [(sql/call :primary-key :tx)]

     ;; FKs
     [(sql/call :foreign-key :token-detail/id) (sql/call :references :TokenDetail :token-detail/id) (sql/raw "ON DELETE CASCADE")]
     ;; FIXME: Disabled due to JobCreated and FundsIn event order (FundsIn comes first, before job has been created)
     ;; [(sql/call :foreign-key :job/id) (sql/call :references :Job :job/id) (sql/raw "ON DELETE CASCADE")]
     ]
    :list-keys []}

   {:table-name :File
    :table-columns
    [[:file/id :integer]
     [:file/hash :varchar]
     [:file/name :varchar]
     [:file/directory-hash :varchar]

     ;; PK
     [(sql/call :primary-key :file/id)]]
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

     [(sql/call :primary-key :event/comparable-id)]]}

   {:table-name :ContractEventCheckpoint
    :table-columns
    [[:id :serial]
     [:checkpoint :json]
     [:created-at :timestamp]
     [(sql/call :primary-key :id)]]}])
