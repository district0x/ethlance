(ns ethlance.shared.smart-contracts-dev)

(def smart-contracts
  {:token {:name "TestToken" :address "0x4c14209D06B72D9E348c479cb01D760fC3d504C9"}
   :test-nft {:name "TestNft" :address "0x42D240cFd590DEf4d59DDb7fE0f40a95A7380F5b"}
   :test-multi-token {:name "TestMultiToken" :address "0x5D133CE4b4b048C9CE431a6d60C6d9ae28C09F60"}
   :ethlance-structs {:name "EthlanceStructs" :address "0x79Fd274CD656fE51b87F2E3D1183b6E13d3ad8D9"}
   :job-helpers {:name "JobHelpers" :address "0x5CaD667fa4e00F6B43DE4F7c1eEC7DBaA939753B"}
   :job {:name "Job" :address "0xc440eebD8af93579FB85274ba75c2e2B6Cfd430b"}
   :mutable-forwarder {:name "MutableForwarder" :address "0xC99090BC95B2AC1995650c046119f43a00Dd7Fd5"}
   :ethlance {:name "EthlanceProxy" :address "0x8D3f5A07dEe4553Fea1007991FcF707f3F9bbB3a" :forwards-to :ethlance-impl}
   :ethlance-impl {:name "Ethlance" :address "0x54c2588282Ac9b5e8eDBedEaf76d12c5cF5a5c5C"}})
