(ns ethlance.ui.component.token-info
  "Component to show token info (ETH, ERC20/721/1155) with USD value and link to contract"
  (:require
    [ethlance.ui.util.tokens :as util.tokens]
    [district.ui.conversion-rates.subs :as rates-subs]
    [re-frame.core :as re]))

(defn token-info-str [token-amount token-detail]
  (let [token-type (:token-detail/type token-detail)
        token-name (:token-detail/name token-detail)
        decimals (:token-detail/decimals token-detail)]
    (str (util.tokens/human-amount token-amount token-type decimals)
         " "
         (if token-name
           token-name
           (clojure.string/upper-case (name (or token-type :?)))))))

; Examples:
;   0.02 ETH ($30, 0x000...)
;   10 TEST (ERC20, 0x76BE3...)
;   1 (ERC721, 0x3xZ...)
;   2 (ERC1155, 0x76BE3...) linking to https://etherscan.io/token/0x76be3b62873462d2142405439777e971754e8e77
(defn c-token-info [token-amount token-detail]
  (let [token-type (:token-detail/type token-detail)
        token-decimals (:token-detail/decimals token-detail)
        display-amount (util.tokens/human-amount token-amount token-type token-decimals)
        token-symbol (:token-detail/symbol token-detail)
        dollar-amount (if (= token-type :eth)
                        (util.tokens/round 2 (* display-amount @(re/subscribe [::rates-subs/conversion-rate :ETH :USD]))))
        display-type (if (= token-type :eth)
                       (str "$" dollar-amount)
                       (clojure.string/upper-case (name (or token-type ""))))
        address (:token-detail/id token-detail)
        short-address (str (subs address 0 5) "...")]
    [:div (str display-amount " " token-symbol)
     [:a
      {:target "_blank" :href (util.tokens/address->token-info-url address)}
      " (" display-type ", " short-address ")"]]))
