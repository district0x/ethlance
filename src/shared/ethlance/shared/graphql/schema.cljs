(ns ethlance.shared.graphql.schema)

(def graphql-schema
  "The main GraphQL Schema"
  "
  #
  # Scalars
  #

  scalar Date
  scalar Keyword

  #
  # Interfaces
  #

  \"Information for Pagination implemented by Listings.\"
  interface PageInfo {
    totalCount: Int
    endCursor: Int
    hasNextPage: Boolean
  }

  # 
  # Begin
  #

  type Query {
    \"Retrieve the User ID for the given Ethereum Address, or null\"
    userId(user_address : ID!): Int

    \"Retrieve the User Data for the User defined by the given User ID\"
    user(user_id : Int!): User

    \"Search for and create User Listings\"
    userSearch(user_address: ID): UserList

    \"Retrieve the Candidate Data defined by the User ID\"
    candidate(user_id : Int!): Candidate
  }

  # User Types

  type User {
    \"User Identifier\"
    user_id: Int

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
  }

  type UserList implements PageInfo {
    items: [User!]
    totalCount: Int
    endCursor: Int
    hasNextPage: Boolean
  }

  # Candidate Types

  type Candidate {
    \"User ID for the given candidate\"
    user_id: Int

    \"Auto Biography written by the Candidate\"
    candidate_biography: String

    \"The date when the Candidate was registered\"
    candidate_dateRegistered: Date

    \"Professional Title Defined by the User\"
    candidate_professionalTitle: String
  }

  type CandidateList implements PageInfo {
    items: [Candidate!]
    totalCount: Int
    endCursor: Int
    hasNextPage: Boolean
  }

  # Employer Types

  type Employer {
    \"User ID for the given employer\"
    user_id: Int

    \"Auto Biography written by the Employer\"
    employer_biography: String

    \"Date of Registration\"
    employer_dateRegistered: Date

    \"Professional Title Defined by the User\"
    employer_professionalTitle: String
  }

  type EmployerList implements PageInfo {
    items: [Employer!]
    totalCount: Int
    endCursor: Int
    hasNextPage: Boolean
  }

  # Arbiter Types

  type Arbiter {
    \"User ID for the given arbiter\"
    user_id: Int
 
    \"Date the Arbiter was registered\"
    arbiter_dateRegistered: Date

    \"Type of currency to get paid in\"
    arbiter_currencyType: Keyword

    \"The amount to be paid based on payment type\"
    arbiter_paymentValue: Int

    \"The payment type\"
    arbiter_paymentType: Keyword
  }

  type ArbiterList implements PageInfo {
    items: [Arbiter!]
    totalCount: Int
    endCursor: Int
    hasNextPage: Boolean
  }

  # Job Types

  type Job {
    \"Identifier for the given Job\"
    job_index: Int
  }

  type JobList implements PageInfo {
    items: [Job!]
    totalCount: Int
    endCursor: Int
    hasNextPage: Boolean
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
  }

  type WorkContractList implements PageInfo {
    items: [WorkContract!]
    totalCount: Int
    endCursor: Int
    hasNextPage: Boolean
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

    \"Date of invoice payment\"
    invoice_amountPaid: Int
  }

  type InvoiceList implements PageInfo {
    items: [Invoice!]
    totalCount: Int
    endCursor: Int
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
  }

  type DisputeList implements PageInfo {
    items: [Dispute!]
    totalCount: Int
    endCursor: Int
    hasNextPage: Boolean
  }

  ")
