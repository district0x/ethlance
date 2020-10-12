(ns ethlance.ui.component.splash-layout
  (:require [ethlance.ui.component.button :refer [c-button c-button-label]]
            [ethlance.ui.component.circle-button :refer [c-circle-icon-button]]
            [ethlance.ui.component.ethlance-logo :refer [c-ethlance-logo]]
            [ethlance.ui.component.icon :refer [c-icon]]
            [ethlance.ui.component.splash-mobile-navigation-bar
             :refer
             [c-splash-mobile-navigation-bar]]
            [ethlance.ui.component.splash-navigation-bar
             :refer
             [c-splash-navigation-bar]]
            [ethlance.ui.util.navigation :as util.navigation]
            [reagent.core :as r]
            ["react-transition-group" :refer [CSSTransition TransitionGroup]]))

(defn c-how-to-card [label src]
  [:div.how-to-card
   [:div.image
    [:img {:src src}]]
   [:span.label label]])

(defn c-how-to-candidate
  []
  [:div.how-to-candidate
   [c-how-to-card "Find A Job" "/images/icon-find.png"]
   [c-how-to-card "Apply For A Job" "/images/icon-applyjob.png"]
   [c-how-to-card "Get Hired" "/images/icon-ok.png"]
   [c-how-to-card "Create Invoices" "/images/icon-tasks.png"]
   [c-how-to-card "Receive Ether" "/images/icon-free-ether.png"]
   [c-how-to-card "Leave Feedback" "/images/icon-feedback.png"]])

(defn c-how-to-employer
  []
  [:div.how-to-employer
   [c-how-to-card "Create A Job" "/images/icon-create.png"]
   [c-how-to-card "Invite Freelancers" "/images/icon-invite.png"]
   [c-how-to-card "Accept Job Proposals" "/images/icon-accept.png"]
   [c-how-to-card "Get Tasks Done" "/images/icon-free-tasks.png"]
   [c-how-to-card "Pay Invoices In Ether" "/images/icon-ether.png"]
   [c-how-to-card "Leave Feedback" "/images/icon-feedback.png"]])

(defn c-how-to-arbiter
  []
  [:div.how-to-arbiter
   [c-how-to-card "Complete Your Profile" "/images/icon-free-tasks.png"]
   [c-how-to-card "Receive Invite For Job" "/images/icon-invite.png"]
   [c-how-to-card "Investigate The Problem" "/images/icon-find.png"]
   [c-how-to-card "Resolve The Dispute" "/images/icon-ok.png"]
   [c-how-to-card "Receive Ether" "/images/icon-free-ether.png"]
   [c-how-to-card "Leave Feedback" "/images/icon-feedback.png"]])

(defn c-how-it-works-layout
  []
  (let [*current-selection (r/atom :candidate)]
    (fn []
      [:div.how-it-works-layout
       [:div.button-listing
        [c-button
         {:color :primary
          :disabled? (not= @*current-selection :candidate)
          :on-click #(reset! *current-selection :candidate)}
         [c-button-label [:span "Freelancer"]]]
        [c-button
         {:color :primary
          :disabled? (not= @*current-selection :employer)
          :on-click #(reset! *current-selection :employer)}
         [c-button-label [:span "Employer"]]]
        [c-button
         {:color :primary
          :disabled? (not= @*current-selection :arbiter)
          :on-click #(reset! *current-selection :arbiter)}
         [c-button-label [:span "Arbiter"]]]]
       [:> TransitionGroup
        {:className "card-transitions"}

        (when (= @*current-selection :candidate)
          [:> CSSTransition
           {:className "active-page"
            :classNames "how-card"
            :timeout 200}
           (r/as-element [c-how-to-candidate])])

        (when (= @*current-selection :employer)
          [:> CSSTransition
           {:className "active-page"
            :classNames "how-card"
            :timeout 200}
           (r/as-element [c-how-to-employer])])

        (when (= @*current-selection :arbiter)
          [:> CSSTransition
           {:className "active-page"
            :classNames "how-card"
            :timeout 200}
           (r/as-element [c-how-to-arbiter])])]])))

(defn c-splash-layout
  []
  [:div.splash-layout.animation-fade-in
   [:div.mobile-header
    [c-splash-mobile-navigation-bar]]
   [:div.header
    [c-splash-navigation-bar]]
   [:div.main-content
    ;; BEGIN BANNER
    [:div.banner.content-center
     [:div.banner-header
      [:h2 "The Future of Work is Now"]
      [:span "Hire or Work for Ether cryptocurrency"]
      [:div.button-listing
       [c-button
        {:color :primary
         :size :auto
         :title "Become a Freelancer"
         :on-click (util.navigation/create-handler {:route :route.me/sign-up :query {:tab :candidate}})
         :href (util.navigation/resolve-route {:route :route.me/sign-up :query {:tab :candidate}})}
        [c-button-label [:span "Become a "] [:b "Freelancer"]]]
       [c-button
        {:color :primary
         :size :auto
         :title "Become an Employer"
         :on-click (util.navigation/create-handler {:route :route.me/sign-up :query {:tab :employer}})
         :href (util.navigation/resolve-route {:route :route.me/sign-up :query {:tab :employer}})}
        [c-button-label [:span "Become an "] [:b "Employer"]]]
       [c-button
        {:color :primary
         :size :auto
         :title "Become an Arbiter"
         :on-click (util.navigation/create-handler {:route :route.me/sign-up :query {:tab :arbiter}})
         :href (util.navigation/resolve-route {:route :route.me/sign-up :query {:tab :arbiter}})}
        [c-button-label [:span "Become an "] [:b "Arbiter"]]]]]

     [:div.banner-figure
      [:figure
       [:img.home-dialog.home-dialog-base {:src "/images/img-top-banner.png"}]
       [:img.home-dialog.home-dialog-1.animation-hovering {:src "/images/svg/home-dialog-1.svg"}]
       [:img.home-dialog.home-dialog-2.animation-hovering {:src "/images/svg/home-dialog-2.svg"}]
       [:img.home-dialog.home-dialog-3.animation-hovering {:src "/images/svg/home-dialog-3.svg"}]
       [:img.home-dialog.home-dialog-4.animation-hovering {:src "/images/svg/home-dialog-4.svg"}]]]

     [:div.banner-footer
      [:figure.img-district
       [:img {:src "/images/district.png"}]]
      [:p "Participate in Ethlance's governance processes:"]
      [:a {:href "http://district0x.io"}
       "Introducing the district0x Network"]]]

    ;; BEGIN WELCOME TO
    [:div.welcome.content-center
     [:h3 "Welcome to"]
     [:h2 "A Smarter Way to Work"]
     [:div.box-listing
      [:div.box-ball
       [:div.vertical-box
        [:figure
         [:img.ic-service {:src "/images/icon-service.png"}]
         [:img.ic-service-center.animation-shrink-fade {:src "/images/ic-service-center.png"}]]
        [:div.text "Decentralized dispute resolution, anyone can become an arbiter."]]]

      [:div.box-separator "+"]

      [:div.box-ball
       [:div.vertical-box
        [:figure
         [:img {:src "/images/icon-decentralised.png"}]
         [:img.ic-circle.ic-circle-1.animation-move-about {:src "/images/svg/circle.svg"}]
         [:img.ic-circle.ic-circle-2.animation-move-about {:src "/images/svg/circle.svg"}]
         [:img.ic-circle.ic-circle-3.animation-move-about {:src "/images/svg/circle.svg"}]
         [:img.ic-circle.ic-circle-4.animation-move-about {:src "/images/svg/circle.svg"}]]
        [:div.text "Fully decentralized on blockchain."]]]

      [:div.box-separator "+"]

      [:div.box-ball
       [:div.vertical-box
        [:figure
         [:img.ic-free-ring.animation-ring-rotate {:src "/images/svg/ring.svg"}]
         [:img.ic-free.animation-tag-rotate {:src "/images/ic-free.png"}]]
        [:div.text "No restrictions and free membership."]]]]]

    [:div.created
     [:div.content-center
      [:h3 "We Created"]
      [:h2 "Ethlance for Everyone!"]
      [:div.flex-box-listing
       [:div.horizontal-box
        [:figure
         [:img {:src "/images/take-no-cut.png"}]]
        [:div.text
         [:h3 "We take no cut"]
         [:p "Ethlance doesn't take a percentage of your earned
        Ether. The amount of Ether the employer pays is the amount the
        freelancer gets."]]]

       [:div.horizontal-box
        [:figure
         [:img {:src "/images/blockchain.png"}]]
        [:div.text
         [:h3 "It's all on blockchain"]
         [:p "The Ethlance database is distributed on the Ethereum
        public blockchain and the sources files are on IPFS. Ethlance
        is accessible to everyone forever, without any central
        authority having control over it."]]]

       [:div.horizontal-box
        [:figure
         [:img {:src "/images/costs.png"}]]
        [:div.text
         [:h3 "No artificial costs or restrictions"]
         [:p "Everybody can apply for, or create, an unlimited number
        of jobs. All that is needed is to pay Ethereum gas fees
        associated with these operations."]]]

       [:div.horizontal-box
        [:figure
         [:img {:src "/images/network.png"}]]
        [:div.text
         [:h3 "Part of the district0x Network"]
         [:p "Ethlance is the first district on the "
          [:a {:href "http://district0x.io"} "district0x Network"]
          ", a collective of decentralized marketplaces and
         communities."]]]]]]

    [:div.checkout.content-center
     [:h3 "Check Out"]
     [:h2 "How Ethlance Works"]
     ;; TODO: owl listing component
     [c-how-it-works-layout]]]


   [:div.footer
    [:div.footer-content
     [:div.header-section
      [:div.logo
       [c-ethlance-logo {:color :white :inline? false}]]
      [:h2 "The Future of Work is Now."]
      [:h2 "Stay up-to-date with Ethlance."]
      ;; TODO: email input component
      [:div.fancy-email
       [:input.form-input {:type "email" :placeholder "Enter Email"}]
       [:span.form-button [c-icon {:name :ic-arrow-right :size :small :color :secondary}]]]]

     [:div.splash-section
      [:img {:src "/images/icon-coins.png"}]]

     [:div.links-section
      [:div.listing
       [:span.title "Learn More"]
       [:a
        {:title "About Us"
         :on-click (util.navigation/create-handler {:route :route.misc/about})
         :href (util.navigation/resolve-route {:route :route.misc/about})}
        [:span "About Us"]]
       [:a
        {:title "How it Works"
         :on-click (util.navigation/create-handler {:route :route.misc/how-it-works})
         :href (util.navigation/resolve-route {:route :route.misc/how-it-works})}
        [:span "How it Works"]]
       [:a {:href "https://blog.district0x.io/"} "Blog"]]
      [:div.listing
       [:span.title "Get Started"]
       [:a
        {:title "Become a Freelancer"
         :on-click (util.navigation/create-handler {:route :route.me/sign-up :query {:tab :candidate}})
         :href (util.navigation/resolve-route {:route :route.me/sign-up :query {:tab :candidate}})}
        [:span "Become a Freelancer"]]
       [:a
        {:title "Become an Employer"
         :on-click (util.navigation/create-handler {:route :route.me/sign-up :query {:tab :employer}})
         :href (util.navigation/resolve-route {:route :route.me/sign-up :query {:tab :employer}})}
        [:span "Become an Employer"]]
       [:a
        {:title "Find Work"
         :on-click (util.navigation/create-handler {:route :route.job/jobs})
         :href (util.navigation/resolve-route {:route :route.job/jobs})}
        [:span "Find Work"]]
       [:a
        {:title "Find Candidates"
         :on-click (util.navigation/create-handler {:route :route.user/candidates})
         :href (util.navigation/resolve-route {:route :route.user/candidates})}
        [:span "Find Candidates"]]]
      [:div.listing
       [:span.title "Connect With Us"]
       [:div.button-listing
        [c-circle-icon-button {:name :facebook :title "District0x Facebook"
                               :size :small :href "https://www.facebook.com/district0x/"}]
        [c-circle-icon-button {:name :twitter :title "District0x Twitter"
                               :size :small :href "https://twitter.com/district0x?lang=en"}]
        [c-circle-icon-button {:name :github :title "District0x Github"
                               :size :small :href "https://github.com/district0x"}]
        [c-circle-icon-button {:name :slack :title "District0x Slack"
                               :size :small :href "https://district0x-slack.herokuapp.com/"}]]]]
     [:div.buttons-section
      [c-button
       {:color :primary :size :auto
        :title "Become a Freelancer"
        :on-click (util.navigation/create-handler {:route :route.me/sign-up :query {:tab :candidate}})
        :href (util.navigation/resolve-route {:route :route.me/sign-up :query {:tab :candidate}})}
       [c-button-label [:span "Become a " [:b "Freelancer"]]]]
      [c-button
       {:color :primary :size :auto
        :title "Become an Employer"
        :on-click (util.navigation/create-handler {:route :route.me/sign-up :query {:tab :employer}})
        :href (util.navigation/resolve-route {:route :route.me/sign-up :query {:tab :employer}})}
       [c-button-label [:span "Become an " [:b "Employer"]]]]
      [c-button
       {:color :primary :size :auto
        :title "Become an Arbiter"
        :on-click (util.navigation/create-handler {:route :route.me/sign-up :query {:tab :arbiter}})
        :href (util.navigation/resolve-route {:route :route.me/sign-up :query {:tab :arbiter}})}
       [c-button-label [:span "Become an " [:b "Arbiter"]]]]]
     [:div.footer-section
      [:span "Copyright © 2020 Ethlance.com. All rights reserved."]]]]])
