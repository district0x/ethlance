(ns ethlance.shared.graphql.schema)

(def schema
  "The main GraphQL Schema"
  "
  #
  # Scalars
  #

  scalar Date
  scalar Keyword


  #
  # Base Types
  #

  enum OrderDirection {
    asc
    desc
  }


  #
  # Begin
  #

  type Query {

    \"Retrieve the User Data for the User defined by the given User ID\"
    user(user_address : ID!): User

    \"Search for and create User Listings\"
    userSearch(
      user_address: ID,
      user_fullName: String,
      user_userName: String,
      orderBy: UserListOrderBy,
      orderDirection: OrderDirection,
      limit: Int,
      offset: Int,
    ): UserList

    \"Retrieve the Candidate Data defined by the User ID\"
    candidate(user_address: ID!): Candidate

    # TODO: Rating
    \"Search for and create Candidate Listings\"
    candidateSearch(
      user_address: ID,
      categoriesAnd: [String!],
      categoriesOr: [String!],
      skillsAnd: [String!],
      skillsOr: [String!],
      professionalTitle: String,
      orderBy: CandidateListOrderBy,
      orderDirection: OrderDirection,
      limit: Int,
      offset: Int,
    ): CandidateList

    \"Retrieve the Employer Data defined by the User ID\"
    employer(user_address : ID!): Employer

    # TODO: Rating
    \"Search for and create Employer Listings\"
    employerSearch(
      user_address: ID,
      professionalTitle: String,
      orderBy: EmployerListOrderBy,
      orderDirection: OrderDirection,
      limit: Int,
      offset: Int,
    ): EmployerList

    \"Retrieve the Arbiter Data defined by the User ID\"
    arbiter(user_address : ID!): Arbiter

    # TODO: Rating
    \"Search for and create Arbiter Listings\"
    arbiterSearch(
      user_address: ID,
      orderBy: ArbiterListOrderBy,
      orderDirection: OrderDirection,
      limit: Int,
      offset: Int,
    ): ArbiterList

    \"Retrieve the Job Data defined by the Job Index\"
    job(job_index : Int!): Job

    \"Search for and create Job Listings\"
    jobSearch(
      job_index: Int,
      orderBy: JobListOrderBy,
      orderDirection: OrderDirection,
      limit: Int,
      offset: Int,
    ): JobList

    \"Retrieve the Work Contract Data defined by the Work Contract Index\"
    workContract(job_index: Int!, workContract_index: Int!): WorkContract

    \"Retrieve the Dispute Data defined by the dispute index\"
    dispute(job_index: Int!,
            workContract_index: Int!,
            dispute_index: Int!): Dispute

    \"Retrieve the Invoice Data defined by the invoice index\"
    invoice(job_index: Int!,
            workContract_index: Int!,
            invoice_index: Int!): Invoice
  }

  type Mutation {
    signIn(input: SignInInput!): String!
  }

  input SignInInput {
    dataSignature: String!,
    data: String!
  }

  # User Types

  type User {

    \"Ethereum Address Corresponding to this Registered User.\"
    user_address: ID

    \"Two Letter Country Code\"
    user_countryCode: String

    \"Full Name of the Given User\"
    user_fullName: String

    \"The short-form username of the User\"
    user_userName: String

    \"Email Address\"
    user_email: String

    \"Profile Picture Assigned to the given User\"
    user_profileImage: String

    \"Date when the user was Registered\"
    user_dateCreated: Date

    \"Date when the user was Last Updated\"
    user_dateUpdated: Date

    \"List of languages the user speaks\"
    user_languages: [String!]

    \"Registration Checks\"
    user_isRegisteredCandidate: Boolean!
    user_isRegisteredArbiter: Boolean!
    user_isRegisteredEmployer: Boolean!
  }

  type UserList {
    items: [User!]
    totalCount: Int
    endCursor: Int
    hasNextPage: Boolean
  }

  enum UserListOrderBy {
    dateUpdated
    dateCreated
  }


  # Candidate Types

  type Candidate {
    \"User ID for the given candidate\"
    user_address: ID

    \"Auto Biography written by the Candidate\"
    candidate_biography: String

    \"The date when the Candidate was registered\"
    candidate_dateRegistered: Date

    \"Professional Title Defined by the User\"
    candidate_professionalTitle: String

    \"Categories of Focused Work\"
    candidate_categories: [String!]

    \"Skills of the Candidate\"
    candidate_skills: [String!]

    \"Feedback for the candidate\"
    candidate_feedback(
      limit: Int,
      offset: Int
    ): FeedbackList!
  }

  type CandidateList {
    items: [Candidate!]
    totalCount: Int
    endCursor: String
    hasNextPage: Boolean
  }

  enum CandidateListOrderBy {
    dateUpdated
    dateCreated
    dateRegistered
  }


  # Employer Types

  type Employer {
    \"User ID for the given employer\"
    user_address: ID

    \"Auto Biography written by the Employer\"
    employer_biography: String

    \"Date of Registration\"
    employer_dateRegistered: Date

    \"Professional Title Defined by the User\"
    employer_professionalTitle: String

    \"Feedback for the employer\"
    employer_feedback(
      limit: Int,
      offset: Int
    ): FeedbackList!
  }

  type EmployerList {
    items: [Employer!]
    totalCount: Int
    endCursor: String
    hasNextPage: Boolean
  }

  enum EmployerListOrderBy {
    dateUpdated
    dateCreated
    dateRegistered
  }


  # Arbiter Types

  type Arbiter {
    \"User ID for the given arbiter\"
    user_address: ID

    \"Date the Arbiter was registered\"
    arbiter_dateRegistered: Date

    \"Type of currency to get paid in\"
    arbiter_currencyType: Keyword

    \"The amount to be paid based on payment type\"
    arbiter_paymentValue: Int

    \"The payment type\"
    arbiter_paymentType: Keyword

    \"Feedback for the arbiter\"
    arbiter_feedback(
      limit: Int,
      offset: Int
    ): FeedbackList!
  }

  type ArbiterList  {
    items: [Arbiter!]
    totalCount: Int
    endCursor: String
    hasNextPage: Boolean
  }

  enum ArbiterListOrderBy {
    dateUpdated
    dateCreated
    dateRegistered
  }


  # Job Types

  type Job {
    \"Identifier for the given Job\"
    job_index: Int
    job_title: String
    job_acceptedArbiterAddress: ID
    job_availability: Keyword
    job_bidOption: Keyword
    job_category: String
    job_description: String
    job_dateCreated: Date
    job_dateStarted: Date
    job_dateFinished: Date
    job_employerAddress: ID
    job_estimatedLengthSeconds: Int
    job_includeEtherToken_: Boolean
    job_isInvitationOnly_: Boolean
    job_rewardValue: Int
    job_workContracts(
      limit: Int,
      offset: Int
    ): WorkContractList
  }

  type JobList {
    items: [Job!]
    totalCount: Int
    endCursor: String
    hasNextPage: Boolean
  }

  enum JobListOrderBy {
    dateCreated
    dateStarted
    dateFinished
  }

  # WorkContract Types

  type WorkContract {
    \"Identifier for the given Job\"
    job_index: Int

    \"Identifier for the given Work Contract\"
    workContract_index: Int

    \"Work Contract Status\"
    workContract_contractStatus: Keyword

    \"Address of the Accepted Candidate\"
    workContract_candidateAddress: ID

    \"Date last updated\"
    workContract_dateUpdated: Date

    \"Date of creation\"
    workContract_dateCreated: Date

    \"Date when the contract was finished\"
    workContract_dateFinished: Date

    \"Invoice Listing for Work Contract\"
    workContract_invoices(
      limit: Int,
      offset: Int,
    ): InvoiceList

    \"Dispute Listing for Work Contract\"
    workContract_disputes(
      limit: Int,
      offset: Int,
    ): DisputeList

    workContract_employerFeedback: Feedback
    workContract_candidateFeedback: Feedback

    workContract_comments(
      limit: Int,
      offset: Int
    ): CommentList
  }

  type WorkContractList {
    items: [WorkContract!]
    totalCount: Int
    endCursor: String
    hasNextPage: Boolean
  }

  enum WorkContractOrderBy {
    dateUpdated
    dateCreated
  }


  # Invoice Types

  type Invoice {
    \"Identifier for the given Job\"
    job_index: Int

    \"Identifier for the given Work Contract\"
    workContract_index: Int

    \"Identifier for the given Invoice\"
    invoice_index: Int

    \"Date of creation\"
    invoice_dateCreated: Date

    \"Date last updated\"
    invoice_dateUpdated: Date

    \"Date the invoice was paid\"
    invoice_datePaid: Date

    \"Amount of pay requested\"
    invoice_amountRequested: Int

    \"Amount of invoice actually paid\"
    invoice_amountPaid: Int

    invoice_comments(
      limit: Int,
      offset: Int,
    ): CommentList
  }

  type InvoiceList  {
    items: [Invoice!]
    totalCount: Int
    endCursor: String
    hasNextPage: Boolean
  }


  # Dispute Types

  type Dispute {
    \"Identifier for the given Job\"
    job_index: Int

    \"Identifier for the given Work Contract\"
    workContract_index: Int

    \"Identifier for the given Dispute\"
    dispute_index: Int

    \"Reason for the Dispute\"
    dispute_reason: String

    \"Date of creation\"
    dispute_dateCreated: Date

    \"Date last updated\"
    dispute_dateUpdated: Date

    \"Date when the dispute was resolved\"
    dispute_dateResolved: Date

    # TODO: incorporate resolution amounts for differing currencies (ERC20)
    \"Amount paid out to employer\"
    dispute_employerResolutionAmount: Int

    \"Amount paid out to candidate\"
    dispute_candidateResolutionAmount: Int

    \"Amount paid out to arbiter\"
    dispute_arbiterResolutionAmount: Int

    dispute_comments(
      limit: Int,
      offset: Int,
    ): CommentList
  }

  type DisputeList {
    items: [Dispute!]
    totalCount: Int
    endCursor: String
    hasNextPage: Boolean
  }

  # Comment Types

  type Comment {
    job_index: Int!
    workContract_index: Int!
    dispute_index: Int
    invoice_index: Int
    comment_index: Int!
    comment_revision: Int!
    user_id: Int
    comment_userType: Keyword
    comment_dateCreated: Date
    comment_text: String
  }

  type CommentList {
    items: [Comment!]
    totalCount: Int
    endCursor: String
    hasNextPage: Boolean
  }

  # Feedback Types

  type Feedback {
    job_index: Int!
    workContract_index: Int!
    feedback_index: Int!
    feedback_toUserType: Keyword!
    feedback_toUserId: Int!
    feedback_fromUserType: Keyword!
    feedback_fromUserAddress: ID!
    feedback_dateCreated: Date
    feedback_rating: Int!
    feedback_text: String
  }

  type FeedbackList {
    items: [Feedback!]
    totalCount: Int
    endCursor: String
    hasNextPage: Boolean
  }

  ")
