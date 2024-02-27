(ns ethlance.shared.smart-contracts-dev)

(def smart-contracts
  {:token {:name "TestToken" :address "0xaBcE6db8dB79c9651dFc0bf78496a5CaE63f5379"}
   :test-nft {:name "TestNft" :address "0x86B08490dc6A78Fc386eF3e017bE40B30C42Eb71"}
   :test-multi-token {:name "TestMultiToken" :address "0x314738b35bceC9e30a837Fd522749421E02089cF"}
   :ethlance-structs {:name "EthlanceStructs" :address "0x7b11a89baF126AA6FaBbcf6f6feF415B89e7996a"}
   :job-helpers {:name "JobHelpers" :address "0xB9cb7D2bd2b0853c7d8bFd95b3992c4B3881F409"}
   :job {:name "Job" :address "0x02d74A6248868141AFbb468D111D306C35F843f8"}
   :mutable-forwarder {:name "MutableForwarder" :address "0x2482d647aa3426D9156AEFb1D61247D961e6A33a"}
   :ethlance {:name "EthlanceProxy" :address "0xF119b2499a673DC88af9F71fF4a79beEd56cf2bc" :forwards-to :ethlance-impl}
   :ethlance-impl {:name "Ethlance" :address "0x45adBbB1e49986b5CbBba084Dd43b3BAE48180d6"}})
