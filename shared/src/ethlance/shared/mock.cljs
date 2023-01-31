(ns ethlance.shared.mock
  "Sets of mock data"
  (:require
   [ethlance.shared.random :as random]
   [ethlance.shared.constants :as constants]))


(def first-names
  #{"Ben"
    "Matus"
    "Filip"
    "Juan"
    "Brady"
    "Peter"
    "Alex"
    "Joe"
    "Jane"
    "John"
    "Barney"})

(def last-names
  #{"Batman"
    "Doe"
    "Lastman"
    "Firstman"
    "Longfoot"
    "Tarley"
    "Joker"
    "Ghetto"
    "Corona"
    "Covid"
    "Spiderman"})

(def first-title-category
  #{"Graphical"
    "Programming"
    "C++"
    "Python"
    "Scientific"
    "Artistic"
    "Business"
    "Game"})

(def second-title-category
  #{"Architect"
    "Programmer"
    "Developer"
    "Engineer"})

(defn generate-job-title
  []
  (str (rand-nth (vec first-title-category))
       " "
       (rand-nth (vec second-title-category))
       (when (random/pick-rand-by-dist [[25 true] [75 false]])
         " Assistant")))

(defn generate-mock-job
  []
  {:id 1
   :title (generate-job-title)
   :description "lorem ipsum"
   :category (rand-nth constants/categories)
   :skills (into #{} (random/rand-nth-n constants/skills (inc (rand-int 5))))
   :date-created ""
   :arbiter nil
   :employer nil
   :payment-type
   (random/pick-rand-by-dist
    [[60 :hourly-rate]
     [20 :fixed-price]
     [20 :annual-salary]])
   :experience-level
   (random/pick-rand-by-dist
    [[30 :novice]
     [60 :professional]
     [10 :expert]])
   :project-length
   (random/pick-rand-by-dist
    [[5  :unknown]
     [15 :months]
     [70 :weeks]
     [10 :days]])
   :availability
   (random/pick-rand-by-dist
    [[20 :part-time]
     [80 :full-time]])
   :country (rand-nth constants/countries)})
