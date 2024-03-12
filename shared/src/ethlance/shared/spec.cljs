(ns ethlance.shared.spec
  (:require
    ;; ["is-ipfs" :as is-ipfs]
    [cljs.spec.alpha :as s]
    [clojure.set :as set]
    [district.validation :refer [length? email? not-neg?]]
    [ethlance.shared.constants :as constants]
    [medley.core :refer [map-kv-vals]]))


(def professional-title? #(length? % 2 80))
(def bio? #(length? % 2000))

(s/def :user/name #(length? % 3 80))
(s/def :user/email email?)
(s/def :user/country (partial contains? (set constants/countries)))
(s/def :user/languages (fn [languages]
                         (and (pos? (count languages))
                              (set/subset? languages (set constants/languages)))))


(s/def :user/languages
  (fn [languages]
    (and (pos? (count languages))
         (set/subset? languages (set constants/languages)))))


;; (s/def :user/profile-image is-ipfs/multihash) ; TODO: figure out how to use is-ipfs
(s/def :user/profile-image string?)


;; Used on ethlance.ui.page.sign-up to validate form fields
;; but fails when there's no logged in user
;; (def ethereum-address-pattern #"^0x([A-Fa-f0-9]{40})$")
;; (s/def :user/id #(re-matches ethereum-address-pattern %))

(s/def :user/id #(or (nil? %) (string? %)))

(s/def :candidate/professional-title professional-title?)
(s/def :candidate/rate not-neg?)
(s/def :candidate/rate-currency-id keyword?)
(s/def :candidate/categories (fn [categories]
                               (and (pos? (count categories))
                                    (set/subset? categories constants/categories))))


(s/def :candidate/categories
  (fn [categories]
    (and (pos? (count categories))
         (set/subset? categories constants/categories))))


(s/def :candidate/skills
  (fn [skills]
    (and (pos? (count skills))
         (<= (count skills) 30)
         (set/subset? skills (set constants/skills)))))


(s/def :candidate/bio bio?)

(s/def :employer/professional-title professional-title?)
(s/def :employer/bio bio?)

(s/def :arbiter/professional-title professional-title?)
(s/def :arbiter/bio bio?)
(s/def :arbiter/fee not-neg?)
(s/def :arbiter/fee-currency-id keyword?)
(s/def :arbiter/date-updated int?)


(s/def :job/title (s/and string? (comp not empty?)))
(s/def :job/required-skills (comp not empty?))


(s/def :page.new-job/create
  (s/keys :req [:job/title :job/required-skills]))


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


(defn validate-keys
  [props]
  (map-kv-vals #(s/valid? %1 %2) props))
