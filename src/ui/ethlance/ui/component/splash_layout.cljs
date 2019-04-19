(ns ethlance.ui.component.splash-layout
  (:require
   [re-frame.core :as rf]

   ;; Ethlance Components
   [ethlance.ui.component.ethlance-logo :refer [c-ethlance-logo]]
   [ethlance.ui.component.button :refer [c-button c-button-label]]
   [ethlance.ui.component.inline-svg :refer [c-inline-svg]]
   [ethlance.ui.component.icon :refer [c-icon]]
   [ethlance.ui.component.circle-button :refer [c-circle-icon-button]]
   [ethlance.ui.component.splash-navigation-bar :refer [c-splash-navigation-bar]]))


(defn c-splash-layout
  []
  [:div.splash-layout
   [:div.header
    [c-splash-navigation-bar]]
   [:div.main-content
    ;; BEGIN BANNER
    [:div.banner
     [:div.part-text
      [:h2 "The Future of Work is Now"]
      [:span "Hire or Work for Ether cryptocurrency"]
      [:div.button-listing
       [c-button
        {:color :primary :size :auto}
        [c-button-label [:span "Become a " [:b "Freelancer"]]]]
       [c-button
        {:color :primary :size :auto}
        [c-button-label [:span "Become an " [:b "Arbiter"]]]]
       [c-button
        {:color :primary :size :auto}
        [c-button-label [:span "Become an " [:b "Employer"]]]]]
      [:div.district
       [:figure.img-district
        [:img {:src "./images/district.png"}]]
       [:p "Participate in Ethlance's governance processes:"]
       [:a {:href "http://district0x.io"}
        "Introducing the district0x Network"]]]
     
     [:div.part-figure
      [:figure
       [:img {:src "./images/img-top-banner.png"}]]]]

    ;; BEGIN WELCOME TO
    [:div.welcome
     [:h3 "Welcome to"]
     [:h2 "A Smarter Way to Work"]
     [:div.box-listing
      [:div.box-ball
       [:div.vertical-box
        [:figure
         [:img {:src "./images/icon-service.png"}]]
        [:div.text "0% service fees."]]]

      [:div.box-ball
       [:div.vertical-box
        [:figure
         [:img {:src "./images/icon-decentralised.png"}]]
        [:div.text "Fully decentralized on blockchain."]]]

      [:div.box-ball
       [:div.vertical-box
        [:figure
         [:img {:src "./images/icon-membership.png"}]]
        [:div.text "No restrictions and free membership."]]]]]
    
    [:div.created
     [:h3 "We Created"]
     [:h2 "Ethlance for Everyone!"]
     [:div.flex-box-listing
      [:div.horizontal-box
       [:figure
        [:img {:src "./images/take-no-cut.png"}]]
       [:div.text
        [:h3 "We take no cut"]
        [:p "Ethlance doesn't take a percentage of your earned
        Ether. The amount of Ether the employer pays is the amount the
        freelancer gets."]]]

      [:div.horizontal-box
       [:figure
        [:img {:src "./images/blockchain.png"}]]
       [:div.text
        [:h3 "It's all on blockchain"]
        [:p "The Ethlance database is distributed on the Ethereum
        public blockchain and the sources files are on IPFS. Ethlance
        is accessible to everyone forever, without any central
        authority having control over it."]]]

      [:div.horizontal-box
       [:figure
        [:img {:src "./images/costs.png"}]]
       [:div.text
        [:h3 "No artificial costs or restrictions"]
        [:p "Everybody can apply for, or create, an unlimited number
        of jobs. All that is needed is to pay Ethereum gas fees
        associated with these operations."]]]

      [:div.horizontal-box
       [:figure
        [:img {:src "./images/network.png"}]]
       [:div.text
        [:h3 "Part of the district0x Network"]
        [:p "Ethlance is the first district on the "
         [:a {:href "http://district0x.io"} "district0x Network"]
         ", a collective of decentralized marketplaces and
         communities."]]]]]
    [:div.checkout]]
   [:div.footer]])
   

     
