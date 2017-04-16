(ns ethlance.pages.about-page
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.misc :as misc :refer [col row paper row-plain link a]]
    [ethlance.styles :as styles]
    [re-frame.core :refer [subscribe dispatch]]
    [ethlance.utils :as u]))

(defn about-page []
  (let [eth-contracts (subscribe [:eth/contracts])]
    (fn []
      [misc/center-layout
       [paper
        [:h2 "About Us"]
        [:p "Ethlance is a first of its kind job market platform, built entirely on blockchain and using only cryptocurrency
    for payments. Thanks to those technologies, the platform can sustainably run with 0% service fees.
    Ethlance will never take a cut from transactions between freelancers and employers."]
        [:p "We're running on the public " [link "https://ethereum.org/" "Ethereum"] " blockchain with our front-end source
    files written in " [link "https://clojurescript.org/" "ClojureScript"] " and served via decentralised file storage using "
         [link "https://ipfs.io/" "IPFS"] ". Ethlance is completely open-source and you can find its
    code on " [link "https://github.com/madvas/ethlance" "github.com/madvas/ethlance"] ". If you found a bug,
    please don't hesitate " [link "to open an issue there" "https://github.com/madvas/ethlance/issues/new"] "."]
        [:p "If you're unsure how to use Ethereum, please see the " [a {:route :how-it-works} "How it works"] " page"]
        [:p "The Ethlance backend logic consists of " (count @eth-contracts) " smart-contracts, deployed on the following addresses:"]
        [:ul
         (for [[key {:keys [:github-page :name :address]}] @eth-contracts]
           [:li
            {:key key}
            [link github-page name] " at " [link (u/etherscan-url address) address]])]]])))
