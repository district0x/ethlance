(ns ethlance.server.contract.ethlance-token-store
  "Macros for EthlanceTokenStore")


(defmacro with-ethlance-token-store
  "Rebinds the functions to use the given address instance for
  EthlanceTokenStore methods."
  [address & body]
  `(binding [ethlance.server.contract.ethlance-token-store/*token-store-key*
             [:ethlance-token-store ~address]]
     ~@body))
