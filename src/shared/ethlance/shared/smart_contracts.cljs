(ns ethlance.shared.smart-contracts) 

(def smart-contracts 
  {:district-config
   {:name "DistrictConfig",
    :address "0x984af105970d82837d99c27631cf1ce9adaf0df5"}
   :ds-guard
   {:name "DSGuard"}
   :ethlance-user
   {:name "EthlanceUser"}
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
   :ethlance-registry
   {:name "EthlanceRegistry"}})
