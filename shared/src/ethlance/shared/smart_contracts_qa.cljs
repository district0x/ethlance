(ns ethlance.shared.smart-contracts-qa)

(def smart-contracts
  {:token {:name "TestToken" :address "0xC7B869260BdB1516a638A3ae05FBb4EF2496849F"}
   :test-nft {:name "TestNft" :address "0x06E8636ee84fe530b06997e33C9Fa9ec7738e27C"}
   :test-multi-token {:name "TestMultiToken" :address "0x09435f3a6c2fE5F5A584Dc7FD0901BebA70Ab19E"}
   :ethlance-structs {:name "EthlanceStructs" :address "0xFd4B3f52cCD68eFB392367cf0AFdd4e8f32f2a4e"}
   :job-helpers {:name "JobHelpers" :address "0xE1e2B5770Dc32F4B361f2D15d544e7Ca76B04a62"}
   :job {:name "Job" :address "0x6F6591AB41C7f5dC2D514c12D7f42dE5fD169aF4"}
   :mutable-forwarder {:name "MutableForwarder" :address "0xeA2370de1d4Ab6b99C7B7420b625bE013200Bb21"}
   :ethlance {:name "EthlanceProxy" :address "0xb9232E80982072316D6Ef3a14B5D9a79cc65aAb1" :forwards-to :ethlance-impl}
   :ethlance-impl {:name "Ethlance" :address "0xFD219a291bD559540d9E50835cD2529a16c56aac"}})
