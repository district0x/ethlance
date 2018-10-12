(ns ethlance.server.contract.ethlance-user
  "Macros for EthlanceUser")


(defmacro with-ethlance-user
  "Rebinds the functions to use the given address instance for
  EthlanceUser methods."
  [address & body]
  `(binding [ethlance.server.contract.ethlance-user/*user-key*
             [:ethlance-user ~address]]
     ~@body))
