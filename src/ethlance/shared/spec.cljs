(ns ethlance.shared.spec
  (:require
    ["is-ipfs" :as is-ipfs]
    [cljs.spec.alpha :as s]
    [clojure.set :as set]
    [district.validation :refer [length? email? not-neg?]]
    [ethlance.shared.constants :as constants]
    [medley.core :refer [map-kv-vals]]))


(def professional-title? #(length? % 2 80))
(def bio? #(length? % 2000))

(s/def :user/name #(length? % 3 80))
(s/def :user/email email?)
(s/def :user/country (partial contains? constants/countries))
(s/def :user/languages (fn [languages]
                         (and (pos? (count languages))
                              (set/subset? languages constants/languages))))
(s/def :user/profile-image is-ipfs/multihash)

(s/def :user/github-code string?)
(s/def :user/github-username (s/nilable string?))
(def ethereum-address-pattern #"^0x([A-Fa-f0-9]{40})$")
(s/def :user/address #(re-matches ethereum-address-pattern %))
(s/def :user/linkedin-code string?)
(s/def :user/linkedin-redirect-uri string?)
(s/def :user/is-registered-candidate boolean?)
(s/def :user/date-updated int?)
(s/def :candidate/date-updated int?)

(s/def :candidate/professional-title professional-title?)
(s/def :candidate/rate not-neg?)
(s/def :candidate/rate-currency-id string?)
(s/def :candidate/categories (fn [categories]
                               (and (pos? (count categories))
                                    (set/subset? categories constants/categories))))

(s/def :candidate/skills (fn [skills]
                           (and (pos? (count skills))
                                (<= (count skills) 30)
                                (set/subset? skills constants/skills))))

(s/def :candidate/bio bio?)

(s/def :employer/professional-title professional-title?)
(s/def :employer/bio bio?)
(s/def :employer/date-updated int?)

(s/def :arbiter/professional-title professional-title?)
(s/def :arbiter/bio bio?)
(s/def :arbiter/fee not-neg?)
(s/def :arbitrer/date-updated int?)


(s/def :page.sign-up/update-candidate
  (s/keys :req [:user/name
                :user/email
                :user/country
                :user/languages
                :candidate/professional-title
                :candidate/rate
                :candidate/categories
                :candidate/skills
                :candidate/bio]))


(s/def :page.sign-up/update-employer
  (s/keys :req [:user/name
                :user/email
                :user/country
                :user/languages
                :employer/professional-title
                :employer/bio]))


(s/def :page.sign-up/update-arbiter
  (s/keys :req [:user/name
                :user/email
                :user/country
                :user/languages
                :arbiter/professional-title
                :arbiter/bio
                :arbiter/fee]))


(defn validate-keys [props]
  (map-kv-vals #(s/valid? %1 %2) props))

