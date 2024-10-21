(ns ethlance.server.new-syncer.handlers
  (:require
    [ethlance.server.syncer.handlers :as old-syncer]))

(def handlers
  {:JobCreated old-syncer/handle-job-created
   :FundsIn (partial old-syncer/handle-job-funds-change +)
   :ArbitersInvited old-syncer/handle-arbiters-invited
   :CandidateAdded old-syncer/handle-candidate-added
   :TestEvent old-syncer/handle-test-event
   :InvoiceCreated old-syncer/handle-invoice-created
   :InvoicePaid old-syncer/handle-invoice-paid
   :DisputeRaised old-syncer/handle-dispute-raised
   :DisputeResolved old-syncer/handle-dispute-resolved
   :QuoteForArbitrationSet old-syncer/handle-quote-for-arbitration-set
   :QuoteForArbitrationAccepted old-syncer/handle-quote-for-arbitration-accepted
   :JobEnded old-syncer/handle-job-ended
   :FundsOut (partial old-syncer/handle-job-funds-change -)})
