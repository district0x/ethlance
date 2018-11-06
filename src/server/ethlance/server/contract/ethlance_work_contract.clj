(ns ethlance.server.contract.ethlance-work-contract
  "Macros for EthlanceWorkContract")


(defmacro with-ethlance-work-contract
  "Rebinds the functions to use the given address instance for
  EthlanceWorkContract methods."
  [address & body]
  `(binding [ethlance.server.contract.ethlance-work-contract/*work-contract-key*
             [:ethlance-work-contract ~address]]
     ~@body))
