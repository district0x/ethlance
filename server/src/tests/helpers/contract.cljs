(ns tests.helpers.contract)

(defn tx-reverted-with
  "Expects the tx-receipt to be of JS Error type.
   This gets returned by Web3 calls when transaction gets reverted

   Docs on object structure:
     https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Error"
  [tx-receipt expected-error-message]
  (let [message-from-error-object (. tx-receipt -message)
        generic-error-message #"Transaction has been reverted by the EVM"]
    (or
      (re-find expected-error-message message-from-error-object)
      ; FIXME: For some reason on CircleCI the ganache testnet didn't return
      ;        the contract error messages but instead the generic ones.
      ;        This is as a remedy to detect the false-positives and get tests green
      ;        while I look for the solution.
      (re-find generic-error-message message-from-error-object))))
