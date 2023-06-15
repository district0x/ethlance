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

    user(user_id : ID!): User

    userSearch(
      user_id: ID,
      user_name: String,
      orderBy: UserListOrderBy
      orderDirection: OrderDirection,
      limit: Int,
      offset: Int,
    ): UserList

    candidate(user_id: ID!): Candidate

    candidateSearch(
      user_id: ID,
      searchParams: CandidateSearchParams
      orderBy: CandidateListOrderBy,
      orderDirection: OrderDirection,
      limit: Int,
      offset: Int,
    ): CandidateList

    employer(user_id : ID!): Employer

    employerSearch(
      user_id: ID,
      professionalTitle: String,
      orderBy: EmployerListOrderBy,
      orderDirection: OrderDirection,
      limit: Int,
      offset: Int,
    ): EmployerList

    arbiter(user_id : ID!): Arbiter

    arbiterSearch(
      user_id: ID,
      orderBy: ArbiterListOrderBy,
      orderDirection: OrderDirection,
      limit: Int,
      offset: Int,
    ): ArbiterList

    job(job_id: ID!): Job

    jobSearch(
      job_id: Int,
      searchParams: JobSearchParams,
      orderBy: JobListOrderBy,
      orderDirection: OrderDirection,
      limit: Int,
      offset: Int,
    ): JobList

    jobStory(jobStory_id: Int!): JobStory
    jobStoryList(jobContract: ID): [JobStory]

    \"Retrieve the Dispute Data defined by the dispute index\"
    dispute(jobStory_id: Int!): Dispute

    \"Retrieve the Invoice Data defined by the invoice message id\"
    invoice(invoice_id: Int!): Invoice
    invoiceSearch(candidate: String, employer: String): InvoiceList
  }

  # Input types

  input JobSearchParams {
    randomString: String
    creator: String
    skills: [String]
    category: String
    feedbackMaxRating: Int
    feedbackMinRating: Int
    minHourlyRate: Int
    maxHourlyRate: Int
    minNumFeedbacks: Int
    paymentType: String
    experienceLevel: String
  }

  input CandidateSearchParams {
    feedbackMinRating: Int
    feedbackMaxRating: Int
    paymentType: String

    categoriesAnd: [String]
    categoriesOr: [String]
    skillsAnd: [String]
    skillsOr: [String]
    professionalTitle: String
  }

  input EmployerInput {
    user_id: ID!
    user_email: String!
    user_name: String!
    user_profileImage: String
    user_githubUsername: String
    user_country: String!
    user_languages: [String!]
    employer_bio: String!
    employer_professionalTitle: String!
  }

  input CandidateInput {
    user_id: ID!
    user_email: String!
    user_name: String!
    user_profileImage: String
    user_githubUsername: String
    user_country: String!
    user_languages: [String!]
    candidate_bio: String!
    candidate_professionalTitle: String!
    candidate_categories: [String!]
    candidate_skills: [String!]
    candidate_rateCurrencyId: Keyword!
    candidate_rate: Int!
  }

  input ArbiterInput {
    user_id: ID!
    user_email: String!
    user_name: String!
    user_profileImage: String
    user_githubUsername: String
    user_country: String!
    user_languages: [String!]
    arbiter_bio: String!
    arbiter_professionalTitle: String!
    arbiter_feeCurrencyId: Keyword!
    arbiter_fee: Int!
  }

  input githubSignUpInput {
   user_id: ID!
   code: String!
  }

  input linkedinSignUpInput {
   user_id: ID!
   code: String!
   redirectUri: String!
  }

  input ProposalInput {
    contract: String!,
    text: String!,
    rate: Float!,
    rateCurrencyId: String # FIXME: remove, job supports only 1 offeredValue
  }

  type Mutation {

    signIn(dataSignature: String!, data: String!): signInPayload!
    sendMessage(jobStory_id: Int!, to: ID!, text: String!, jobStoryMessage_type: Keyword, message_type: Keyword): Boolean!,
    leaveFeedback(jobStory_id: Int!, text: String, rating: Int!, to: ID!): Boolean!,
    updateEmployer(input: EmployerInput!): updateEmployerPayload!,
    updateCandidate(input: CandidateInput!): updateCandidatePayload!,
    updateArbiter(input: ArbiterInput!): updateArbiterPayload!,
    createJobProposal(input: ProposalInput): JobStory,
    removeJobProposal(jobStory_id: ID!): JobStory,
    replayEvents: Boolean!,
    githubSignUp(input: githubSignUpInput!): githubSignUpPayload!
    linkedinSignUp(input: linkedinSignUpInput!): linkedinSignUpPayload!
  }

  # mutation result types

  type signInPayload {
    jwt: String!
    user_id: String!
  }

  type updateCandidatePayload {
    user_id: ID!
    user_dateUpdated: Date!
    user_profileImage: String
    user_githubUsername: String
    user_linkedinUsername: String
    candidate_dateUpdated: Date!
  }

  type updateEmployerPayload {
    user_id: ID!
    user_dateUpdated: Date!
    user_profileImage: String
    user_githubUsername: String
    user_linkedinUsername: String
    employer_dateUpdated: Date!
  }

  type updateArbiterPayload {
    user_id: ID!
    user_dateUpdated: Date!
    user_profileImage: String
    user_githubUsername: String
    user_linkedinUsername: String
    arbiter_dateUpdated: Date!
  }

  type githubSignUpPayload {
    user_id: ID!
    user_name: String
    user_githubUsername: String
    user_email: String
    user_country: String
  }

  type linkedinSignUpPayload {
    user_id: ID!
    user_name: String
    user_linkedinUsername: String
    user_email: String
    user_country: String
  }

  # User Types

  type User {

    \"Ethereum Address Corresponding to this Registered User.\"
    user_id: ID

    \"Two Letter Country Code\"
    user_country: String

    \"Full Name of the Given User\"
    user_name: String

    user_githubUsername: String

    user_linkedInUsername: String

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
    user_languages: [String]

    \"Registration Checks\"
    user_isRegisteredCandidate: Boolean!
    user_isRegisteredArbiter: Boolean!
    user_isRegisteredEmployer: Boolean!
  }

  type UserList {
    items: [User]
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
    user_id: ID
    user: User

    \"Auto Biography written by the Candidate\"
    candidate_bio: String

    candidate_rating: Float

    \"The date when the Candidate was registered\"
    candidate_dateRegistered: Date

    \"Professional Title Defined by the User\"
    candidate_professionalTitle: String

    \"Categories of Focused Work\"
    candidate_categories: [String]

    \"Skills of the Candidate\"
    candidate_skills: [String]

    candidate_rateCurrencyId: Keyword

    candidate_rate: Int

    \"Feedback for the candidate\"
    candidate_feedback(
      limit: Int,
      offset: Int
    ): FeedbackList

    jobStories: JobStoryList
  }

  type CandidateList {
    items: [Candidate]
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
    user_id: ID
    user: User

    \"Auto Biography written by the Employer\"
    employer_bio: String
    employer_rating: Float

    \"Date of Registration\"
    employer_dateRegistered: Date

    \"Professional Title Defined by the User\"
    employer_professionalTitle: String

    \"Feedback for the employer\"
    employer_feedback(
      limit: Int,
      offset: Int
    ): FeedbackList

    jobStories: JobStoryList
  }

  type EmployerList {
    items: [Employer]
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
    user_id: ID
    user: User
    arbiter_dateRegistered: Date
    arbiter_professionalTitle: String
    arbiter_bio: String
    arbiter_rating: Float
    arbiter_feeCurrencyId: Keyword
    arbiter_fee: Int
    arbiter_feedback(
      limit: Int,
      offset: Int
    ): FeedbackList
    arbitrations: ArbitrationList
  }

  type Arbitration {
    user_id: ID
    job_id: String
    arbitration_dateArbiterAccepted: String
    arbitration_fee: Int
    arbitration_feeCurrencyId: String
    arbitration_status: String
    job: Job
  }

  type ArbitrationList  {
    items: [Arbitration]
    totalCount: Int
    endCursor: String
    hasNextPage: Boolean
  }

  type ArbiterList  {
    items: [Arbiter]
    totalCount: Int
    endCursor: String
    hasNextPage: Boolean
  }

  enum ArbiterListOrderBy {
    dateUpdated
    dateCreated
    dateRegistered
  }


  type TokenDetails {
    tokenDetail_id: ID!
    tokenDetail_name: String
    tokenDetail_symbol: String
    tokenDetail_abi: String
  }
  # Job Types

  type Job {
    job_id: ID
    job_title: String
    job_description: String
    job_requiredSkills: [String]
    job_requiredExperienceLevel: String
    job_category: String
    job_status: Keyword
    job_dateCreated: Float # TODO: change back to Date after switching to district-ui-graphql
    job_dateUpdated: Float # TODO: change back to Date after switching to district-ui-graphql

    job_tokenType: String
    job_tokenAmount: Float
    job_tokenAddress: String
    job_tokenId: Int
    tokenDetails: TokenDetails

    job_acceptedArbiterAddress: String
    job_employerAddress: String

    job_employer: Employer
    job_arbiter: Arbiter

    jobStories(limit: Int, offset: Int): JobStoryList
    invoices(limit: Int, offset: Int): InvoiceList
    invoice(invoice_id: Int!, job_id: String!): Invoice

    job_estimatedProjectLength: String
    job_maxNumberOfCandidates: Int
    job_invitationOnly: Boolean
    job_requiredAvailability: String
    job_bidOption: String
  }


  type JobList {
    items: [Job]
    totalCount: Int
    endCursor: String
    hasNextPage: Boolean
  }

  enum JobListOrderBy {
    dateCreated
    dateStarted
    dateFinished
  }

  type JobStory {
    jobStory_id: ID
    job_id: String
    job: Job
    candidate: Candidate
    jobStory_status: Keyword
    jobStory_candidate: String
    jobStory_dateCreated: Date
    jobStory_dateUpdated: Date

    jobStory_employerFeedback: [Feedback]  # This job's feedback for employer
    jobStory_candidateFeedback: [Feedback] # This job's Feedback for candidate
    jobStory_arbiterFeedback: [Feedback]   # This job's Feedback for arbiter

    jobStory_dispute: Dispute

    invitationMessage: JobStoryMessage
    invitationAcceptedMessage: JobStoryMessage
    proposalMessage: JobStoryMessage
    proposalAcceptedMessage: JobStoryMessage
    directMessages: [DirectMessage]

    jobStory_invoices(limit: Int, offset: Int): InvoiceList

    # The below fields were ethlanceJobStory_...
    jobStory_proposalRate: Float
    jobStory_proposalRateCurrencyId: Int
    jobStory_dateContractActive: Date
    jobStory_dateArbiterAccepted: Date
  }

  type JobStoryList {
    items: [JobStory]
    totalCount: Int
    endCursor: String
    hasNextPage: Boolean
  }

  enum JobStoryOrderBy {
    dateUpdated
    dateCreated
  }

  # Invoice Types

  type Invoice {
    id: ID # Unique composite id consisting of <job-id>-<invoice-id>
           # Because invoice-id is only unique within Job smart contract context
           # But one Job smart contract can have multiple JobStory DB models associated to it
    \"Identifier for the given Job\"
    job_id: String

    \"Identifier for the given JobStory\"
    jobStory_id: Int
    jobStory: JobStory

    invoice_status: String

    \"Identifier for the given Invoice\"
    invoice_id: Int

    \"Date the invoice was paid\"
    invoice_datePaid: Date

    \"Date the invoice was requested\"
    invoice_dateRequested: Date

    \"Amount of pay requested\"
    invoice_amountRequested: Float

    \"Amount of invoice actually paid\"
    invoice_amountPaid: Float

    invoice_hoursWorked: Int
    invoice_hourlyRate: Int

    creationMessage: JobStoryMessage
    paymentMessage: JobStoryMessage
    disputeRaisedMessage: JobStoryMessage
    disputeResolvedMessage: JobStoryMessage
  }

  type InvoiceList  {
    items: [Invoice]
    totalCount: Int
    endCursor: String
    hasNextPage: Boolean
  }


  # Dispute Types

  type Dispute {
    \"Identifier for the given Job\"
    job_id: String

    \"Identifier for the given JobStory\"
    jobStory_id: ID

    \"Reason for the Dispute\"
    dispute_reason: String

    \"Resolution for the Dispute\"
    dispute_resolution: String

    \"Date of creation\"
    dispute_dateCreated: Date

    \"Date when the dispute was resolved\"
    dispute_dateResolved: Date

  }

  # Feedback Types

  type Feedback {
    message_id: ID
    job_id: Int
    jobStory_id: Int
    message: JobStoryMessage

    feedback_toUserType: Keyword
    feedback_toUserAddress: String
    feedback_fromUser: User
    feedback_fromUserType: Keyword
    feedback_fromUserAddress: String
    feedback_dateCreated: Date
    feedback_rating: Int
    feedback_text: String
  }

  type FeedbackList {
    items: [Feedback]
    totalCount: Int
    endCursor: String
    hasNextPage: Boolean
  }

  interface Message {
    message_id: ID
    message_text: String
    message_type: String
    message_creator: String
    message_dateCreated: Float
  }

  type DirectMessage implements Message {
    message_id: ID
    message_text: String
    message_type: String
    message_dateCreated: Float
    message_creator: String
    directMessage_recipient: String
    recipient: User
    creator: User
  }

  type JobStoryMessage implements Message {
    message_id: ID
    message_text: String
    message_type: String
    message_creator: String
    message_dateCreated: Float
    creator: User
    jobStoryMessageType: String
  }
  ")
