(ns ethlance.server.contract.ethlance-job
  "Macros for EthlanceJob")


(defmacro with-ethlance-job
  "Rebinds the functions to use the given address instance for
  EthlanceJob methods."
  [address & body]
  `(binding [ethlance.server.contract.ethlance-job/*job-key*
             [:ethlance-job ~address]]
     ~@body))
