(ns ethlance.shared.graphql.schema)

(def graphql-schema
  "The main GraphQL Schema"
  "
  scalar Date
  scalar Keyword

  type Query {
    hello: String
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
