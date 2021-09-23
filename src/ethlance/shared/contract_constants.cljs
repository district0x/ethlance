(ns ethlance.shared.contract-constants)

(def job-type {:gig 0 :bounty 1})
(def token-type {:eth 0 :erc20 1 :erc721 2 :erc1155 3})
(def operation-type {:one-step-job-creation 0 :two-step-job-creation 1 :add-funds 2})
; Corresponds to the enum TargetMethod values in Job contract
(def job-target-method {:accept-quote-for-arbitration 0 :add-funds 1 :add-funds-and-pay-invoice 2})
