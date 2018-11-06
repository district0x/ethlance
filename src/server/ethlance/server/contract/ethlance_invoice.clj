(ns ethlance.server.contract.ethlance-invoice
  "Macros for EthlanceInvoice")


(defmacro with-ethlance-invoice
  "Rebinds the functions to use the given `address` instance for
  EthlanceInvoice methods."
  [address & body]
  `(binding [ethlance.server.contract.ethlance-invoice/*invoice-key*
             [:ethlance-invoice ~address]]
     ~@body))
