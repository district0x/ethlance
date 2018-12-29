(ns ethlance.server.contract.multi-linked-list
  "Macros for MultiLinkedList")


(defmacro with-multi-linked-list
  "Rebinds the functions to use the given address instance for
  MultiLinkedList methods."
  [address & body]
  `(binding [ethlance.server.contract.multi-linked-list/*multi-linked-list-key*
             ~address]
     ~@body))
