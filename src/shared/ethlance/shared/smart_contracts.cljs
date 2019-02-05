(ns ethlance.shared.smart-contracts) 


(def smart-contracts 
  {:ds-guard
   {:name "DSGuard"}
   :ethlance-user
   {:name "EthlanceUser"}
   :ethlance-invoice
   {:name "EthlanceInvoice"}
   :ethlance-dispute
   {:name "EthlanceDispute"}
   :ethlance-work-contract
   {:name "EthlanceWorkContract"}
   :ethlance-user-factory
   {:name "EthlanceUserFactory"}
   :ethlance-user-factory-fwd
   {:name "MutableForwarder"}
   :ethlance-job-factory
   {:name "EthlanceJobFactory"}
   :ethlance-job-factory-fwd
   {:name "MutableForwarder"}
   :ethlance-job-store
   {:name "EthlanceJobStore"}
   :ethlance-token-store
   {:name "EthlanceTokenStore"}
   :ethlance-registry
   {:name "EthlanceRegistry"}
   :ethlance-comment
   {:name "EthlanceComment"}
   :ethlance-feedback
   {:name "EthlanceFeedback"}
   :test-token
   {:name "TestToken"}})
