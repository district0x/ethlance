(ns ethlance.shared.smart-contracts-qa)

(def smart-contracts
  {:token {:name "TestToken" :address "0x0000000000000000000000000000000000000000"}
   :test-nft {:name "TestNft" :address "0x0000000000000000000000000000000000000000"}
   :test-multi-token {:name "TestMultiToken" :address "0x0000000000000000000000000000000000000000"}
   :ethlance-structs {:name "EthlanceStructs" :address "0x0000000000000000000000000000000000000000"}
   :job-helpers {:name "JobHelpers" :address "0x0000000000000000000000000000000000000000"}
   :job {:name "Job" :address "0x0000000000000000000000000000000000000000"}
   :mutable-forwarder {:name "MutableForwarder" :address "0x0000000000000000000000000000000000000000"}
   :ethlance {:name "EthlanceProxy" :address "0x0000000000000000000000000000000000000000" :forwards-to :ethlance-impl}
   :ethlance-impl {:name "Ethlance" :address "0x0000000000000000000000000000000000000000"}})
