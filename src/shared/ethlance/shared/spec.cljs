(ns ethlance.shared.spec
  "Includes the data specs for all ethlance data"
  (:require
   [bignumber.core :as bn]
   [clojure.spec.alpha :as s]
   [ethlance.shared.enum.currency-type :refer [enum-currency]]
   [ethlance.shared.enum.bid-option :refer [enum-bid]]
   [ethlance.shared.enum.contract-status :refer [enum-status]]
   [ethlance.shared.enum.payment-type :refer [enum-payment]]
   [ethlance.shared.enum.availability :refer [enum-availability]]
   [ethlance.shared.enum.user-type :refer [enum-user-type]]))


;;
;; Fundamental Specs
;;

(s/def ::id pos-int?)
(s/def ::index nat-int?)
(s/def ::address string?) ;; TODO: better address spec
(s/def ::datetime nat-int?)
(s/def ::ipfs-hash string?) ;; TODO: better hash spec(?)
(s/def ::email string?) ;; TODO: better email predicate
(s/def ::bigint (s/or :bn (s/and bn/bignumber? bn/int?)
                      :n (s/and number? int?)))

;; Enumerations

(s/def ::currency-type (set (keys enum-currency)))
(s/def ::bid-option (set (keys enum-bid)))
(s/def ::contract-status (set (keys enum-status)))
(s/def ::payment-type (set (keys enum-payment)))
(s/def ::availability (set (keys enum-availability)))
(s/def ::user-type (set (keys enum-user-type)))

;;
;; User Data
;;

(s/def :user/id ::id)
(s/def :user/address ::address)
(s/def :user/country-code (s/and string? #(= (count %) 2)))
(s/def :user/email ::email)
(s/def :user/profile-image ::ipfs-hash)
(s/def :user/date-created ::datetime)
(s/def :user/date-updated ::datetime)
(s/def :github/api-key string?)
(s/def :linkedin/api-key string?)
(s/def :user/languages (s/coll-of string? :distinct true :into []))


;;
;; User Candidate Data
;;

(s/def :candidate/biography string?)
(s/def :candidate/date-registered ::datetime)
(s/def :candidate/professional-title string?)
(s/def :candidate/categories (s/coll-of string? :distinct true :into []))
(s/def :candidate/skills (s/coll-of string? :distinct true :into []))


;;
;; User Employer Data
;;

(s/def :employer/biography string?)
(s/def :employer/date-registered ::datetime)
(s/def :employer/professional-title string?)


;;
;; User Arbiter Data
;;

(s/def :arbiter/biography string?)
(s/def :arbiter/date-registered ::datetime)
(s/def :arbiter/currency-type ::currency-type)
(s/def :arbiter/payment-value ::bigint)


;;
;; Job Data
;;

(s/def :job/index ::index)
(s/def :job/title string?)
(s/def :job/accepted-arbiter ::address)
(s/def :job/availability ::availability)
(s/def :job/bid-option ::bid-option)
(s/def :job/category string?)
(s/def :job/description string?)
(s/def :job/date-created ::datetime)
(s/def :job/date-started ::datetime)
(s/def :job/date-finished (s/nilable ::datetime))
(s/def :job/employer-address ::address)
(s/def :job/estimated-length-seconds nat-int?)
(s/def :job/include-ether-token? boolean?)
(s/def :job/is-invitation-only? boolean?)
(s/def :job/reward-value ::bigint)

;; Arbiter Request
(s/def :arbiter-request/date-requested ::datetime)
(s/def :arbiter-request/is-employer-request? boolean?)

(s/def ::arbiter-request (s/keys :req [:user/id
                                       :job/index
                                       :arbiter-request/date-requested
                                       :arbiter-request/is-employer-request?]))

(s/def :job/arbiter-requests (s/coll-of ::arbiter-request :distinct true))
(s/def :job/skills (s/coll-of string? :distrinct true :into []))


;;
;; Work Contract
;;

(s/def :work-contract/index ::index)
(s/def :work-contract/candidate-address ::address)
(s/def :work-contract/contract-status ::contract-status)
(s/def :work-contract/date-updated ::datetime)
(s/def :work-contract/date-created ::datetime)
(s/def :work-contract/date-finished ::datetime)


;;
;; Invoice
;;

(s/def :invoice/index ::index)
(s/def :invoice/date-created ::datetime)
(s/def :invoice/date-updated ::datetime)
(s/def :invoice/date-paid ::datetime)
(s/def :invoice/amount-requested ::bigint)
(s/def :invoice/amount-paid (s/nilable ::bigint))


;;
;; Dispute
;;

(s/def :dispute/index ::index)
(s/def :dispute/reason string?)
(s/def :dispute/date-created ::datetime)
(s/def :dispute/date-updated ::datetime)
(s/def :dispute/date-resolved (s/nilable ::datetime))
(s/def :dispute/employer-resolution-amount (s/nilable ::bigint))
(s/def :dispute/candidate-resolution-amount (s/nilable ::bigint))
(s/def :dispute/arbiter-resolution-amount (s/nilable ::bigint))


;;
;; Comment
;;

(s/def :comment/index ::index)
(s/def :comment/revision ::index)
(s/def :comment/user-type ::user-type)
(s/def :comment/date-created ::datetime)
(s/def :comment/text string?)
