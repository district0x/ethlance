(ns ethlance.server.contract.ethlance-job-store
  "Macros for EthlanceJobStore")


(defmacro with-ethlance-job-store
  "Rebinds the functions to use the given address instance for
  EthlanceJobStore methods."
  [address & body]
  `(binding [ethlance.server.contract.ethlance-job-store/*job-store-key*
             [:ethlance-job-store ~address]]
     ~@body))
