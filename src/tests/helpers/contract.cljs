(ns tests.helpers.contract)

(defn tx-reverted-with
  "Expects the tx-receipt to be of JS Error type.
   This gets returned by Web3 calls when transaction gets reverted

   Docs on object structure:
     https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Error"
  [tx-receipt expected-error-message]
  (re-find expected-error-message (. tx-receipt -message)))
