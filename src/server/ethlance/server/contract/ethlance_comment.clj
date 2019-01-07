(ns ethlance.server.contract.ethlance-comment
  "Macros for EthlanceComment")


(defmacro with-ethlance-comment
  "Rebinds the functions to use the given address instance for
  EthlanceComment methods."
  [address & body]
  `(binding [ethlance.server.contract.ethlance-comment/*comment-key*
             [:ethlance-comment ~address]]
     ~@body))
