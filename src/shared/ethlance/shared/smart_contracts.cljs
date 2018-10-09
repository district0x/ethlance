(ns ethlance.shared.smart-contracts) 

(def smart-contracts 
  {:district-config
   {:name "DistrictConfig",
    :address "0x984af105970d82837d99c27631cf1ce9adaf0df5"}
   :ds-guard
   {:name "DSGuard"}
   :ethlance-user
   {:name "EthlanceUser"}
   :ethlance-job
   {:name "EthlanceJob"}
   :ethlance-user-factory
   {:name "EthlanceUserFactory"}
   :ethlance-user-factory-fwd
   {:name "MutableForwarder"}
   :ethlance-job-factory
   {:name "EthlanceJobFactory"}
   :ethlance-job-factory-fwd
   {:name "MutableForwarder"}
   :ethlance-registry
   {:name "EthlanceRegistry"}
   :ethlance-registry-fwd
   {:name "MutableForwarder"}})
