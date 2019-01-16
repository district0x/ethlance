(ns ethlance.server.contract.ethlance-feedback
  "Macros for EthlanceFeedback")


(defmacro with-ethlance-feedback
  "Rebinds the functions to use the given address instance for
  EthlanceFeedback methods."
  [address & body]
  `(binding [ethlance.server.contract.ethlance-feedback/*feedback-key*
             [:ethlance-feedback ~address]]
     ~@body))
