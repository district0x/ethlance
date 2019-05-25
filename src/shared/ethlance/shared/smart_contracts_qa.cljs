(ns ethlance.shared.smart-contracts-qa)

(def smart-contracts
  {:ds-guard
   {:name "DSGuard" :address "0x0000000000000000000000000000000000000000"}

   :ethlance-registry
   {:name "EthlanceRegistry" :address "0x0000000000000000000000000000000000000000"}
   
   :ethlance-user
   {:name "EthlanceUser" :address "0x0000000000000000000000000000000000000000"}
   
   :ethlance-user-factory
   {:name "EthlanceUserFactory" :address "0x0000000000000000000000000000000000000000"}
   
   :ethlance-user-factory-fwd
   {:name "MutableForwarder" :address "0x0000000000000000000000000000000000000000" :forwards-to "ethlance-user-factory"}
   
   :ethlance-comment
   {:name "EthlanceComment" :address "0x0000000000000000000000000000000000000000"}
   
   :ethlance-feedback
   {:name "EthlanceFeedback" :address "0x0000000000000000000000000000000000000000"}
   
   :ethlance-invoice
   {:name "EthlanceInvoice" :address "0x0000000000000000000000000000000000000000"}
   
   :ethlance-dispute
   {:name "EthlanceDispute" :address "0x0000000000000000000000000000000000000000"}
   
   :ethlance-token-store
   {:name "EthlanceTokenStore" :address "0x0000000000000000000000000000000000000000"}
   
   :ethlance-work-contract
   {:name "EthlanceWorkContract" :address "0x0000000000000000000000000000000000000000"}
   
   :ethlance-job-store
   {:name "EthlanceJobStore" :address "0x0000000000000000000000000000000000000000"}
   
   :ethlance-job-factory
   {:name "EthlanceJobFactory" :address "0x0000000000000000000000000000000000000000"}
   
   :ethlance-job-factory-fwd
   {:name "MutableForwarder" :address "0x0000000000000000000000000000000000000000" :forwards-to "ethlance-job-factory"}
   
   :token
   {:name "TestToken" :address "0x0000000000000000000000000000000000000000"}})
