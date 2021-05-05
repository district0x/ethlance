(ns ethlance.shared.ipfs-files-spec)

{:ethlance/job-created
 {:job/title "string, max 100 chars"
  :job/description "string, max 5000 chars"
  :job/category "one of ethlance.shared.constants/categories"
  :job/expertise-level "beginner|intermediate|expert"
  :job/bid-option "hourly|monthly|annually"
  :job/required-availability "part-time|full-time"
  :job/estimated-length "hours-or-days|weeks|months|lt-6-months"
  :job/required-skills "one or more of ethlance.shared.constants/skills"}

 :ethlance/candidate-added
 {:message/text "string, max 3000 chars"}

 :ethlance/invoice-created
 {:invoice/hours-worked "float"
  :message/text "string, max 3000 chars"}

 :ethlance/invoice-paid
 {:message/text "string, max 3000 chars"}

 :ethlance/invoice-canceled
 {:message/text "string, max 3000 chars"}

 :ethlance/dispute-raised
 {:message/text "string, max 3000 chars"}

 :ethlance/dispute-resolved
 {:message/text "string, max 3000 chars"}}


