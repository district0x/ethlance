(ns ethlance.shared.graphql-mutations-spec)

{:mutation/sign-in
 {:data "string"
  :data-signature "string"}

 :mutation/update-employer
 {:user/email "email"
  :user/name "string, max 80 chars"
  :user/country "one of ethlance.shared.constants/countries"
  :user/languages "one or more of ethlance.shared.constants/languages"
  :user/profile-image "ipfs hash"
  :employer/bio "string, max 5000 chars"
  :employer/professional-title "string, max 150 chars"}


 :mutation/update-candidate
 {:user/email "email"
  :user/name "string, max 80 chars"
  :user/country "one of ethlance.shared.constants/countries"
  :user/languages "one or more of ethlance.shared.constants/languages"
  :user/profile-image "ipfs hash"
  :candidate/bio "string, max 5000 chars"
  :candidate/categories "one or more of ethlance.shared.constants/categories"
  :candidate/skills "one or more of ethlance.shared.constants/skills"
  :candidate/available? "bool"}


 :mutation/update-arbiter
 {:user/email "email"
  :user/name "string, max 80 chars"
  :user/country "one of ethlance.shared.constants/countries"
  :user/languages "one or more of ethlance.shared.constants/languages"
  :user/profile-image "ipfs hash"
  :arbiter/bio "string, max 5000 chars"
  :arbiter/categories "one or more of ethlance.shared.constants/categories"
  :arbiter/skills "one or more of ethlance.shared.constants/skills"
  :arbiter/available? "bool"}


 :mutation/update-job
 {:job/address "address"
  :job/title "string, max 100 chars"
  :job/description "string, max 5000 chars"
  :job/category "one of ethlance.shared.constants/categories"
  :job/expertise-level "beginner|intermediate|expert"
  :job/bid-option "hourly|monthly|annually"
  :job/required-availability "part-time|full-time"
  :job/estimated-length "hours-or-days|weeks|months|lt-6-months"
  :job/required-skills "one or more of ethlance.shared.constants/skills"}


 :mutation/add-job-invitation
 {:job/address "address"
  :user/address "address"
  :message/text "string, max 5000 chars"}

 :mutation/add-job-application
 {:job/address "address"
  :message/text "string, max 5000 chars"}

 :mutation/add-job-message
 {:job/address "address"
  :message/text "string, max 5000 chars"}

 :mutation/add-job-feedback
 {:job/address "address"
  :message/text "string, max 5000 chars"}

 :mutation/close-job
 {:job/address "address"}

 :mutation/github-sign-up
 ;; Already implemented
 {}

 :mutation/linkedin-sign-up
 ;; Already implemented
 {}





























 }


