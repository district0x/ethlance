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

    user(user_address : ID!): User

    userSearch(
      user_address: ID,
      user_fullName: String,
      user_userName: String,
      orderBy: UserListOrderBy,
      orderDirection: OrderDirection,
      limit: Int,
      offset: Int,
    ): UserList

    candidate(user_address: ID!): Candidate

    # TODO: Rating
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

    employer(user_address : ID!): Employer

    # TODO: Rating
    employerSearch(
      user_address: ID,
      professionalTitle: String,
      orderBy: EmployerListOrderBy,
      orderDirection: OrderDirection,
      limit: Int,
      offset: Int,
    ): EmployerList

    arbiter(user_address : ID!): Arbiter

    # TODO: Rating
    arbiterSearch(
      user_address: ID,
      orderBy: ArbiterListOrderBy,
      orderDirection: OrderDirection,
      limit: Int,
      offset: Int,
    ): ArbiterList

    job(job_id : Int!): Job

    jobSearch(
      job_id: Int,
      orderBy: JobListOrderBy,
      orderDirection: OrderDirection,
      limit: Int,
      offset: Int,
    ): JobList

    contract(job_id: Int!, contract_id: Int!): Contract

    \"Retrieve the Dispute Data defined by the dispute index\"
    dispute(job_id: Int!,
            contract_id: Int!,
            dispute_id: Int!): Dispute

    \"Retrieve the Invoice Data defined by the invoice index\"
    invoice(job_id: Int!,
            contract_id: Int!,
            invoice_id: Int!): Invoice
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
    user_dateRegistered: Date

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
    dateRegistered
  }


  # Candidate Types

  type Candidate {
    \"User ID for the given candidate\"
    user_address: ID

    \"Auto Biography written by the Candidate\"
    candidate_bio: String

    \"The date when the Candidate was registered\"
    candidate_dateRegistered: Date

    \"Professional Title Defined by the User\"
    candidate_professionalTitle: String

    \"Categories of Focused Work\"
    candidate_categories: [String!]

    \"Skills of the Candidate\"
    candidate_skills: [String!]

    candidate_rateCurrencyId: Keyword

    candidate_rate: Int

    \"Feedback for the candidate\"
    candidate_feedback(
      limit: Int,
      offset: Int
    ): FeedbackList
  }

  type CandidateList {
    items: [Candidate!]
    totalCount: Int
    endCursor: String
    hasNextPage: Boolean
  }

  enum CandidateListOrderBy {
    dateUpdated
    dateRegistered
  }


  # Employer Types

  type Employer {
    \"User ID for the given employer\"
    user_address: ID

    \"Auto Biography written by the Employer\"
    employer_bio: String

    \"Date of Registration\"
    employer_dateRegistered: Date

    \"Professional Title Defined by the User\"
    employer_professionalTitle: String

    \"Feedback for the employer\"
    employer_feedback(
      limit: Int,
      offset: Int
    ): FeedbackList
  }

  type EmployerList {
    items: [Employer!]
    totalCount: Int
    endCursor: String
    hasNextPage: Boolean
  }

  enum EmployerListOrderBy {
    dateUpdated
    dateRegistered
  }


  # Arbiter Types

  type Arbiter {
    \"User ID for the given arbiter\"
    user_address: ID

    \"Date the Arbiter was registered\"
    arbiter_dateRegistered: Date

    arbiter_bio: String

    arbiter_feeCurrencyId: Keyword

    arbiter_fee: Int

    \"Feedback for the arbiter\"
    arbiter_feedback(
      limit: Int,
      offset: Int
    ): FeedbackList
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
    job_id: Int
    job_title: String
    job_acceptedArbiterAddress: ID
    job_status: Keyword
    job_bidOption: Keyword
    job_category: String
    job_description: String
    job_dateCreated: Date
    job_datePublished: Date
    job_dateUpdated: Date
    job_employerAddress: ID
    job_estimatedLength: Int
    job_isInvitationOnly: Boolean
    job_reward: Int
    job_contracts(
      limit: Int,
      offset: Int
    ): ContractList
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

  type Contract {
    \"Identifier for the given Job\"
    job_id: Int

    \"Identifier for the given Contract\"
    contract_id: Int

    \"Contract Status\"
    contract_status: Keyword

    \"Address of the Accepted Candidate\"
    contract_candidateAddress: ID

    \"Date of creation\"
    contract_dateCreated: Date

    \"Date last updated\"
    contract_dateUpdated: Date

    contract_employerFeedback: Feedback
    contract_candidateFeedback: Feedback

    contract_invoices(
      limit: Int,
      offset: Int,
    ): InvoiceList

    contract_disputes(
      limit: Int,
      offset: Int,
    ): DisputeList

  }

  type ContractList {
    items: [Contract!]
    totalCount: Int
    endCursor: String
    hasNextPage: Boolean
  }

  enum ContractOrderBy {
    dateUpdated
    dateCreated
  }


  # Invoice Types

  type Invoice {
    \"Identifier for the given Job\"
    job_id: Int

    \"Identifier for the given Contract\"
    contract_id: Int

    \"Identifier for the given Invoice\"
    invoice_id: Int

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
    job_id: Int

    \"Identifier for the given Contract\"
    contract_id: Int

    \"Reason for the Dispute\"
    dispute_reason: String

    \"Date of creation\"
    dispute_dateCreated: Date

    \"Date when the dispute was resolved\"
    dispute_dateResolved: Date

  }

  type DisputeList {
    items: [Dispute!]
    totalCount: Int
    endCursor: String
    hasNextPage: Boolean
  }

  # Feedback Types

  type Feedback {
    message_id: Int!
    job_id: Int
    contract_id: Int
    feedback_toUserType: Keyword
    feedback_toUserAddress: ID
    feedback_fromUserType: Keyword
    feedback_fromUserAddress: ID
    feedback_dateCreated: Date
    feedback_rating: Int
    feedback_text: String
  }

  type FeedbackList {
    items: [Feedback!]
    totalCount: Int
    endCursor: String
    hasNextPage: Boolean
  }

  ")
