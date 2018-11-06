(ns ethlance.server.contract.ethlance-dispute
  "Macros for EthlanceDispute")


(defmacro with-ethlance-dispute
  "Rebinds the functions to use the given address instance for
  EthlanceDispute methods."
  [address & body]
  `(binding [ethlance.server.contract.ethlance-dispute/*dispute-key*
             [:ethlance-dispute ~address]]
     ~@body))
