(ns ethlance.pages.home-page
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a]]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn landing-button [props]
  [ui/raised-button
   (r/merge-props
     {:style styles/landing-button
      :background-color :transparent
      :label-color "#FFF"}
     props)])

(defn landing-hero []
  [:div {:style styles/landing-bg}
   [row-plain
    {:center "xs"
     :bottom "sm"
     :middle "xs"
     :style styles/full-height}
    [col
     {:xs 12 :sm 9 :md 8 :lg 7}
     [:img {:src "./images/ethereum.svg"
            :style {:margin-bottom 39}}]
     [:h1.sm-font-2-7em.md-font-3-7em
      {:style styles/landing-title}
      "The future of work is now"]
     [:h3.bolder.sm-font-1-5em.md-font-1-7em
      {:style styles/landing-subtitle}
      "hire or work for Ether cryptocurrency"]
     [row-plain
      {:center "xs"
       :style {:margin-top 32}}
      [landing-button
       {:label "Become a Freelancer"
        :href (u/path-for :freelancer/create)}]
      [landing-button
       {:label "Become an Employer"
        :href (u/path-for :employer/create)}]]
     [row-plain
      {:center "xs"
       :style {:margin-top 10
               :margin-bottom 5}}
      [landing-button
       {:label "Find Work"
        :href (u/path-for :search/jobs)}]
      [landing-button
       {:label "Find Candidates"
        :href (u/path-for :search/freelancers)}]]]
    [col
     {:xs 12
      :class "sm-visible"}
     [row-plain
      {:center "xs"}
      [ui/paper
       {:style styles/landing-banner}
       [row
        {:between "xs" :middle "xs"}
        [col {:xs 4}
         [row
          {:middle "xs" :around "xs"}
          [col
           {:xs 12 :sm 4}
           [:img {:style styles/landing-menu-icon-smaller
                  :src "./images/zero-percent.svg"}]]
          [col
           {:xs 12 :sm 8}
           [:h3.black.bolder {:style styles/landing-menu-title} "0% service fees"]]]]
        [col {:xs 4}
         [row
          {:middle "xs" :around "xs"}
          [col
           {:xs 12 :sm 4}
           [:img {:style styles/landing-menu-icon-smaller
                  :src "./images/decentralised.svg"}]]
          [col
           {:xs 12 :sm 7 :md 8}
           [:h3.black.bolder {:style styles/landing-menu-title} "Fully decentralised" [:br] "on blockchain"]]]]
        [col {:xs 4}
         [row
          {:middle "xs" :around "xs"}
          [col
           {:xs 12 :sm 4}
           [:img {:style styles/landing-menu-icon-smaller
                  :src "./images/free-tag2.svg"}]]
          [col
           {:xs 12 :sm 7 :md 8}
           [:h3.black.bolder {:style styles/landing-menu-title} "No restrictions" [:br] "Free membership"]]]]]]]]]])

(defn feature-no-cut []
  [row-plain
   {:middle "xs" :center "xs"
    :style styles/feature-no-cut}
   [col
    {:xs 9 :sm 7 :md 5 :lg 4
     :style styles/text-left}
    [:h1.black
     {:style styles/landing-feature-title}
     "We take no cut!"]
    [:h3.black
     {:style styles/landing-feature-text}
     "Ethlance doesn’t take a percentage of your earned Ether.
     The amount of Ether the employer pays is the amount the freelancer gets."]]
   [col
    {:xs 5 :sm 3 :md 3 :lg 2}
    [:img
     {:src "./images/coins-cloud.svg"
      :style styles/landing-feature-image}]]])

(defn feature-blockchain []
  [row-plain
   {:middle "xs" :center "xs"
    :style styles/feature-blockchain}
   [col
    {:xs 5 :sm 3 :md 3 :lg 2}
    [:img
     {:src "./images/ethereum.png"
      :style styles/landing-feature-image}]]
   [col
    {:xs 9 :sm 6 :sm-offset 1 :md 5
     :style styles/text-left}
    [:h1.black
     {:style styles/landing-feature-title}
     "It’s all on blockchain!"]
    [:h3.black
    {:style styles/landing-feature-text}
    "The Ethlance database is distributed on the Ethereum public blockchain and the source files are on IPFS.
    Ethlance is accessible to everyone forever, without any central authority having control over it."]]])

(defn feature-no-restrictions []
  [row-plain
   {:middle "xs" :center "xs"
    :style styles/feature-no-restrictions}
   [col
    {:xs 9 :sm 8 :md 4
     :style styles/text-left}
    [:h1.black
     {:style styles/landing-feature-title}
     "No artificial costs or restrictions"]
    [:h3.black
     {:style styles/landing-feature-text}
     "Everybody can apply for, or create, an unlimited number of jobs.
    All that is needed is to pay Ethereum gas fees associated with these operations."]]
   [col
    {:xs 10 :sm 8 :md 6 :lg 5}
    [:img
     {:src "./images/to-the-top.svg"
      :style styles/landing-feature-image}]]])

(defn feature-district0x []
  [row-plain
   {:middle "xs" :center "xs"
    :style styles/feature-district0x}
   [col
    {:xs 9 :sm 6 :md 4 :lg 4 :first "sm"}
    [:img
     {:src "./images/district0x.png"
      :style styles/landing-feature-image}]]
   [col
    {:first "xs" :xs 9 :sm 7 :md 5 :md-offset 1 :lg 5
     :style styles/text-left}
    [:h1.black
     {:style styles/landing-feature-title}
     "Part of the district0x Network"]
    [:h3.black
     {:style styles/landing-feature-text}
     "Ethlance is the first district on the "
     [:a {:href "https://district0x.io"
          :target :_blank
          :style (merge styles/underline-text
                        styles/color-inherit)} "district0x Network"]
     ", a collective of decentralized marketplaces and communities."]]])

(def employer-path
  [["job.svg" "Create Job"]
   ["freelancer-search.svg" "Invite Freelancers"]
   ["job-proposal-accept.svg" "Accept Job Proposals"]
   ["tasks-done.svg" "Get Tasks Done"]
   ["invoice-paid.svg" "Pay Invoices in Ether"]
   ["feedback.svg" "Leave Feedback"]])

(def freelancer-path
  [["job-search.svg" "Find Job"]
   ["job-apply.svg" "Apply for a Job"]
   ["badge.svg" "Get Hired"]
   ["invoice.svg" "Create Invoices"]
   ["coins.svg" "Receive Ether"]
   ["feedback.svg" "Leave Feedback"]])

(defn process-diagram []
  [row-plain
   {:center "xs"
    :style styles/process-diagram}
   [col {:xs 12}
    [:h1.black {:style styles/text-center} "How it works"]]
   [col {:xs 12}
    [row
     {:center "xs"
      :style {:padding-top "10px"}}
     (for [[title path] [["Become a Freelancer" freelancer-path] ["Become an Employer" employer-path]]]
       [col
        {:xs 12 :sm 6 :md 4 :lg 3 :key (ffirst path)}
        [:div
         {:style {:padding styles/desktop-gutter}}
         [:h2.black {:style {:margin-bottom "35px" :margin-top "35px" :font-style "italic"}}
          title]
         (for [[src title] path]
           [:div
            {:key src}
            [:img {:style styles/diagram-icon
                   :src (str "./images/" src)}]
            [:h4 title]
            (when-not (= src (first (last path)))
              [:img {:style styles/diagram-arrow-icon
                     :src "./images/arrow.svg"}])])]])]]])

(defn footer-link [route text & [props]]
  [:div
   [:a
    (r/merge-props
      {:href (if (keyword? route) (u/path-for route) route)
       :style {:color "rgba(255, 255, 255, 0.75)"}}
      props)
    text]])

(defn footer []
  [row-plain
   {:around "sm"
    :center "xs"
    :style {:background-color "#181a1a"
            :padding-top 42
            :padding-bottom 27
            :line-height "1.8em"}}
   [col
    {:xs 12 :sm 4
     :class "hoverlinks"}
    [ui/subheader {:style styles/footer-subheader}
     "LEARN MORE"]
    [footer-link :about "About us"]
    [footer-link :how-it-works "How it works"]
    [footer-link "https://blog.ethlance.com" "Blog"]]
   [col
    {:xs 12 :sm 4
     :class "hoverlinks"}
    [ui/subheader {:style styles/footer-subheader}
     "GET STARTED"]
    [footer-link :freelancer/create "Become a Freelancer"]
    [footer-link :employer/create "Become an Employer"]
    [footer-link :search/jobs "Find Work"]
    [footer-link :search/freelancers "Find Candidates"]]
   [col
    {:xs 12 :sm 4
     :class "hoverlinks"}
    [ui/subheader {:style styles/footer-subheader}
     "REACH US"]
    [footer-link "https://www.facebook.com/ethlance/" "Facebook" {:target :_blank}]
    [footer-link "https://twitter.com/ethlance" "Twitter" {:target :_blank}]
    [footer-link "https://github.com/madvas/ethlance" "Github" {:target :_blank}]
    [footer-link "https://district0x-slack.herokuapp.com/" "Slack" {:target :_blank}]]
   [col {:xs 12}
    [misc/logo {:style styles/ethlance-logo-footer}]
    [:div {:style (merge styles/footer-subheader
                         {:font-size "0.9em"
                          :line-height "1.2em"
                          :margin-top 44})}
     "Copyright © 2017 Ethlance.com. All rights reserved."]]])

(defn home-page []
  (let [current-page (subscribe [:db/current-page])]
    (fn []
      [:div
       [ui/app-bar {:show-menu-icon-button false
                    :title-style {:display :none}
                    :style styles/landing-app-bar}
        [row-plain
         {:center "xs"
          :class "sm-absolute md-font-1em sm-margin-top-5 md-margin-top-10"
          :style styles/app-bar-top-banner}
         [:a
          {:href "https://district0x.io"
           :style styles/color-inherit
           :target :_blank}
          "Participate in Ethlance's governance processes: "
          [:span
           {:style styles/underline-text}
           "Introducing the district0x Network"]]]
        [row
         {:style styles/app-bar-home-page}
         [col {:xs 6}
          [misc/logo
           {:on-click (fn []
                        (when (= (:handler @current-page) :home)
                          (dispatch [:window/scroll-to-top])))}]]
         [col {:xs 6}
          [misc/how-it-works-app-bar-link]]]]
       [landing-hero]
       [feature-no-cut]
       [feature-blockchain]
       [feature-no-restrictions]
       [feature-district0x]
       [process-diagram]
       [footer]])))
