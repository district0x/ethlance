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
    [:p "Ethlance is running on the " [link "https://ethereum.org/" "Ethereum"] " public blockchain,
    therefore you'll need the " [link "https://metamask.io/" "MetaMask"] " browser extension to be able to make
    changes into the blockchain. See our video tutorials, where everything is clearly explained!"]
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
     ". The most common way is to register at one of the worldwide
    cryptocurrency exchanges that exchange fiat currency for cryptocurrency. Note that exchange from
    Bitcoin to Ether can be done directly in " [link "https://metamask.io/" "MetaMask"] " or "
     [link "https://github.com/ethereum/mist" "Mist browser"] "."]
    [:h3.bolder "Why do I have to pay Ethereum gas fees?"]
    [:p "Every time you'll want to change something in the Ethlance database you'll be asked to pay a small fee (usually a couple of cents)
     called \"gas fees\". This fee is used to compensate for the electricity costs of the computers running the Ethereum blockchain.
     Thanks to this, Ethlance doesn't need to rent servers and therefore keeps service fees as low as 0%! This fee is a
     great protection against spam. " [:u "Gas fees are by no means profit of Ethlance"] "."]
    [:h3.bolder "What if an employer didn't pay for my work?"]
    [:p "Ethlance holds no responsibility for resolving disputes between freelancers and employers. " [:br]
     "These are some of our guidelines to prevent such situations:"]
    [:ul
     [:li "Read all past feedback of a freelancer/employer."]
     [:li "Send invoices to an employer frequently. Don't continue with your work until a previous invoice is paid."]
     [:li "Check the balance on his or her wallet whether there's enough money to pay you."]
     [:li "Establish good communication with a freelancer/employer."]]
    [:h3.bolder "Why does it take so long after I submit a form?"]
    [:p "The average block time on Ethereum is around 15 seconds. In practice this means you'll wait around 15 to 30
    seconds until you get a confirmation. Occasionally, however, the wait time can be several minutes. Note that while your
     form is submitted into the blockchain you can freely browse the website. You will always be notified
     when your data has been processed. Avoid refreshing the page."]
    [a {:route :about} "About Us"]]])
