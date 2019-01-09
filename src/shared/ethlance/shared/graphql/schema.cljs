(ns ethlance.shared.graphql.schema)

(def graphql-schema
  "The main GraphQL Schema"
  "
  scalar Date
  scalar Keyword

  type Query {
    \"Retrieve the User ID for the given Ethereum Address, or null\"
    userId(user_address : ID!): Int

    \"Retrieve the User Data for the User defined by the given User ID\"
    user(user_id : Int!): User
  }

  type User {
    \"User Identifier\"
    user_id: Int

    \"Ethereum Address Corresponding to this Registered User.\"
    user_address: ID

    \"Two Letter Country Code\"
    user_countryCode: String

  }

  ")
