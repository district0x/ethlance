(ns ethlance.pages.how-it-works-page
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.misc :as misc :refer [col row paper row-plain link a youtube]]
    [ethlance.styles :as styles]))

(defn video [{:keys [:title :src]}]
  [row-plain
   {:center "xs"}
   [:h2
    {:style (merge styles/margin-bottom-gutter
                   styles/margin-top-gutter-more)}
    title]
   [youtube {:src src}]])

(defn how-it-works-page []
  [misc/center-layout
   [paper
    [:h2 "How it works?"]
    [:p "Ethlance is running on " [link "https://ethereum.org/" "Ethereum"] " public blockchain,
    therefore you'll need " [link "https://metamask.io/" "MetaMask"] " browser extension to be able to make
    changes into a blockchain. See our video tutorials, where everything is clearly explained!"]
    [video
     {:title "Installing MetaMask Chrome Extension"
      :src "https://www.youtube.com/embed/gUZ_XT0a9_U?list=PL4rQUoitSeEH8ybx-yM1ocuvF9OvSVkU4"}]
    [video
     {:title "Become Freelancer and Apply for a Job"
      :src "https://www.youtube.com/embed/gKZsEeISvZ8?list=PL4rQUoitSeEH8ybx-yM1ocuvF9OvSVkU4"}]
    [video
     {:title "Become Employer and Create a Job "
      :src "https://www.youtube.com/embed/sxJlVUvlWwg?list=PL4rQUoitSeEH8ybx-yM1ocuvF9OvSVkU4"}]
    [video
     {:title "Creating Invoice and Leaving Feedback to Employer"
      :src "https://www.youtube.com/embed/p873UP7kwxs?list=PL4rQUoitSeEH8ybx-yM1ocuvF9OvSVkU4"}]
    [video
     {:title "Paying Invoice and Leaving Feedback to Freelancer"
      :src "https://www.youtube.com/embed/trIlOfko3KE?list=PL4rQUoitSeEH8ybx-yM1ocuvF9OvSVkU4"}]

    [:h1
     {:style (merge styles/margin-top-gutter-more
                    styles/text-center
                    styles/margin-bottom-gutter)}
     "Frequently Asked Questions"]
    [:h3.bolder "How do I get Ether (Ξ) cryptocurrency?"]
    [:p "Obtaining Ether is very similar to obtaining " [link "https://en.wikipedia.org/wiki/Bitcoin" "Bitcoin"]
     ". Most common way is to register at one of worldwide
    cryptocurrency exchanges and they will exchange your fiat currency into cryptocurrency. Note, that exchange from
    Bitcoin into Ether can be done directly in " [link "https://metamask.io/" "MetaMask"] " or "
     [link "https://github.com/ethereum/mist" "Mist browser"] "."]
    [:h3.bolder "Why do I have to pay Ethereum gas fees?"]
    [:p "Everytime you'll want to change something in Ethlance database you'll be asked to pay small fee (usually couple of cents)
     called \"gas fees\". These money are used to compensate for electricity costs of computers running Ethereum blockchain.
     Thanks to this, Ethlance doesn't need to rent servers and therefore keep service fees as low as 0%! It it also
     great protection against spam. " [:u "Note, money from gas fees are by no means profit of Ethlance"] "."]
    [:h3.bolder "What if employer didn't pay for my work?"]
    [:p "Ethlance holds no responsibility for resolving disputes between freelancers and employers. " [:br]
     "These are some of our advices on how to prevent such situations:"]
    [:ul
     [:li "Read all past feedback of a freelancer/employer."]
     [:li "Send invoices to an employer frequently. Don't continue with work until previous invoice is paid."]
     [:li "See balance on his/her wallet, whether there is enough money to pay you."]
     [:li "Establish good communication with a freelancer/employer."]]
    [:h3.bolder "Why does it take so long after I submit a form?"]
    [:p "Ethereum average block time is around 15 seconds. That means, in practice you'll wait around 15-30
    seconds until you get confirmation. However, occasionally this can be even several minutes. While your
     form is being submitted into blockchain, you can freely browse website, you will always be notified
     when your data has been processed. Avoid refreshing page."]
    [a {:route :about} "About Us"]]])