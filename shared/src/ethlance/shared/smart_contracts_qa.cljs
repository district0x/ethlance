(ns ethlance.shared.smart-contracts-qa)


; Contract addresses deployed to Base Sepolia testnet
; FIXME: currently QA deploy reads these although works against Base (and not Arbitrum)
;        It should be changed either by
;           1) setting ETHLANCE_ENV=qa-base during ui & server compilation
;           2) or change truffle config and when deploying contracts use ETHLANCE_ENV=qa
;              during contract compilation & deployment
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

; Contract addresses deployed to Arbitrum Sepolia testnet
; (def smart-contracts
;   {:token {:name "TestToken" :address "0xC7B869260BdB1516a638A3ae05FBb4EF2496849F"}
;    :test-nft {:name "TestNft" :address "0x06E8636ee84fe530b06997e33C9Fa9ec7738e27C"}
;    :test-multi-token {:name "TestMultiToken" :address "0x09435f3a6c2fE5F5A584Dc7FD0901BebA70Ab19E"}
;    :ethlance-structs {:name "EthlanceStructs" :address "0xFd4B3f52cCD68eFB392367cf0AFdd4e8f32f2a4e"}
;    :job-helpers {:name "JobHelpers" :address "0xE1e2B5770Dc32F4B361f2D15d544e7Ca76B04a62"}
;    :job {:name "Job" :address "0x6F6591AB41C7f5dC2D514c12D7f42dE5fD169aF4"}
;    :mutable-forwarder {:name "MutableForwarder" :address "0xeA2370de1d4Ab6b99C7B7420b625bE013200Bb21"}
;    :ethlance {:name "EthlanceProxy" :address "0xb9232E80982072316D6Ef3a14B5D9a79cc65aAb1" :forwards-to :ethlance-impl}
;    :ethlance-impl {:name "Ethlance" :address "0xFD219a291bD559540d9E50835cD2529a16c56aac"}})
