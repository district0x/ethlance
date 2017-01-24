(ns ethlance.pages.how-it-works-page
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.misc :as misc :refer [col row paper row-plain link]]
    [ethlance.styles :as styles]))

(defn how-it-works-page []
  [misc/center-layout
   [paper
    [:h2 "How it works?"]
    [:p "Etlance database is fully distributed on " [link "https://ethereum.org/" "Ethereum"] " public blockchain,
    therefore you'll need a special tool to be able to interact with it."]
    [:p "You can choose 1 of those 2 ways to join " [link "https://ethereum.org/" "Ethereum"] " network:"]
    [:ol
     [:li
      {:style styles/margin-bottom-gutter-less}
      "Download Chrome extension " [link "https://metamask.io/" "MetaMask"] ". This is more lightweight solution and
     it's super easy to use!" [:br] " Simply add it into Chrome and create your new wallet. After that, you'll need to
      fund it with little bit of Ether (Ξ), so you are able to pay gas fees for given operations. This wallet will
      be automatically visible to Ethlance and you can use it to create your account"]
     [:li "Download " [link "https://github.com/ethereum/mist" "Mist browser"] ".  This is official Ethereum application.
       It's a complete solution for using " [link "https://ethereum.org/" "Ethereum"] ", but before you can start
       using it, it needs to download blockchain (several gigabytes) into your computer. After setting up Mist and
       funding your wallet with some Ether (Ξ), try to open Ethlance inside Mist. To make your wallet visible to Ethlance,
       click in a right top corner \"Connect\", and choose your wallet. Now you should be able to use Ethlance with that
       wallet."]]
    [:h3.bolder
     {:style styles/margin-top-gutter-more}
     "How do I get Ether (Ξ) cryptocurrency?"]
    [:p "Obtaining Ether is very similar to obtaining " [link "https://en.wikipedia.org/wiki/Bitcoin" "Bitcoin"]
     ". Most common way is to register at one of worldwide
    cryptocurrency exchanges and they will exchange your fiat currency into cryptocurrency. Note, that exchange from
    Bitcoin into Ether can be done directly in " [link "https://github.com/ethereum/mist" "Mist browser"] "."]
    [:h3.bolder "Why do I have to pay Ethereum gas fees?"]
    [:p "Everytime you'll want to change something in Ethlance database you'll be asked to pay small fee (usually couple of cents)
     called \"gas fees\". These money are used to compensate for electricity costs of computers running Ethereum blockchain.
     Thanks to this, Ethlance doesn't need to rent servers and therefore keep service fees as low as 0%! It it also
     great protection against spam. " [:u "Note, money from gas fees are by no means profit of Ethlance"] "."]
    [:h3.bolder "What if employer didn't pay for my work?"]
    [:p "Ethlance holds no responsibility for resolving conflicts between freelancers and employers. " [:br]
     "These are some of our advices on how to prevent such conflicts:"]
    [:ul
     [:li "Read all past feedback of a freelancer/employer."]
     [:li "Send invoices to employer frequently. Don't continue with work until previous invoice is paid."]
     [:li "See balance on his/her wallet, whether there is enough money to pay you."]
     [:li "Establish good communication with a freelancer/employer."]]]])