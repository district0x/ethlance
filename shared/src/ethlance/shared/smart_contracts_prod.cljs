(ns ethlance.shared.smart-contracts-prod)

;; FIXME: currently copied from smart-contracts-qa-base.
;;        To be replaced during smart contract deployment to prod environment
(def smart-contracts
  {:token {:name "TestToken" :address "0xEC0eBe108Cb18E2e3E086D95ec0e54186515D28c"}
   :test-nft {:name "TestNft" :address "0xba626CF0E8DE6589DCC3366a83A328c2EfF1aB16"}
   :test-multi-token {:name "TestMultiToken" :address "0x1427FFB41C054FE0a46Fd815eEa38A25D853c89F"}
   :ethlance-structs {:name "EthlanceStructs" :address "0xB7BD1c448b5Bf92C3Bb6Fa67b972fa4a822e9C4D"}
   :job-helpers {:name "JobHelpers" :address "0x997A67E6Ca104bbFB441cb852aDC1d9f93a330Ba"}
   :job {:name "Job" :address "0xf886242c1A9767189eFc95d9863706dD86698CA8"}
   :mutable-forwarder {:name "MutableForwarder" :address "0xB220d6e2f1CCFa96f197bf735368b06B9587a48f"}
   :ethlance {:name "EthlanceProxy" :address "0x7118b32BFCB003deB251f4265bfC2783C64696a8" :forwards-to :ethlance-impl}
   :ethlance-impl {:name "Ethlance" :address "0x9cDb54eF2e9bC741be383Aed1A9cD63e1537DBFd"}})
