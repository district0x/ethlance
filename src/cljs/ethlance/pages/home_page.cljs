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
     {:xs 12 :sm 9 :md 7 :lg 6}
     [:img {:src "./images/ethereum.svg"
            :style {:margin-bottom 10}}]
     [:h1
      {:style styles/landing-title}
      "Future of work is now"]
     [:h3.bolder
      {:style styles/landing-subtitle}
      "hire or work for Ether cryptocurrency"]
     [row-plain
      {:center "xs"
       :style {:margin-top 15}}
      [landing-button
       {:label "Become Freelancer"
        :href (u/path-for :freelancer/create)}]
      [landing-button
       {:label "Become Employer"
        :href (u/path-for :employer/create)}]]
     [row-plain
      {:center "xs"
       :style {:margin-top 10}}
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
    {:xs 10 :sm 7 :md 5 :lg 4
     :style styles/text-left}
    [:h1.black "We take no cut!"]
    [:h3.black "Ethlance doesn’t take a percentage of your earned Ether.
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
    {:xs 8 :sm 6 :sm-offset 1 :md 5
     :style styles/text-left}
    [:h1.black "It’s all on blockchain!"]
    [:h3.black "The Ethlance database is distributed on the Ethereum public blockchain and the source files are on IPFS.
    This makes it accessible to everyone forever, without any central authority having control over it."]]])

(defn feature-no-restrictions []
  [row-plain
   {:middle "xs" :center "xs"
    :style styles/feature-no-restrictions}
   [col
    {:xs 8 :sm 8 :md 4
     :style styles/text-left}
    [:h1.black "No artificial restrictions!"]
    [:h3.black "Everybody can apply for, or create, an unlimited number of jobs.
    All that is needed is to pay Ethereum gas fees associated with these operations."]]
   [col
    {:xs 10 :sm 8 :md 6 :lg 5}
    [:img
     {:src "./images/to-the-top.svg"
      :style styles/landing-feature-image}]]])

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
    [:h1.black {:style styles/text-center} "How it works?"]]
   [col {:xs 12}
    [row
     {:center "xs"}
     (for [[title path] [["Become Freelancer" freelancer-path] ["Become Employer" employer-path]]]
       [col
        {:xs 12 :sm 6 :md 4 :lg 3 :key (ffirst path)}
        [:div
         {:style {:padding styles/desktop-gutter}}
         [:h2.black {:style styles/margin-bottom-gutter}
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
            :padding-top styles/desktop-gutter
            :padding-bottom "40px"}}
   [col
    {:xs 12 :sm 4}
    [ui/subheader {:style styles/footer-subheader}
     "LEARN MORE"]
    [footer-link :about "About us"]
    [footer-link :how-it-works "How it works?"]
    [footer-link "https://blog.ethlance.com" "Blog"]]
   [col
    {:xs 12 :sm 4}
    [ui/subheader {:style styles/footer-subheader}
     "GET STARTED"]
    [footer-link :freelancer/create "Become Freelancer"]
    [footer-link :employer/create "Become Employer"]
    [footer-link :search/jobs "Find Work"]
    [footer-link :search/freelancers "Find Candidates"]]
   [col
    {:xs 12 :sm 4}
    [ui/subheader {:style styles/footer-subheader}
     "REACH US"]
    [footer-link "https://www.facebook.com/ethlance/" "Facebook" {:target :_blank}]
    [footer-link "https://twitter.com/ethlance" "Twitter" {:target :_blank}]
    [footer-link "https://github.com/madvas/ethlance" "Github" {:target :_blank}]
    [footer-link "https://ethlance-slack.herokuapp.com/" "Slack" {:target :_blank}]]
   [col {:xs 12}
    [misc/logo {:style styles/ethlance-logo-footer}]
    [:div {:style (merge styles/footer-subheader
                         {:font-size "0.9em"
                          :margin-top 5})}
     "Copyright © 2017 Ethlance.com. All rights reserved."]]])

(defn home-page []
  (let [current-page (subscribe [:db/current-page])]
    (fn []
      [:div
       [ui/app-bar {:title (r/as-element
                             [misc/logo
                              {:on-click (fn []
                                           (when (= (:handler @current-page) :home)
                                             (dispatch [:window/scroll-to-top])))}])
                    :icon-element-right (r/as-element [misc/how-it-works-app-bar-link])
                    :show-menu-icon-button false
                    :style styles/landing-app-bar}]
       [landing-hero]
       [feature-no-cut]
       [feature-blockchain]
       [feature-no-restrictions]
       [process-diagram]
       [footer]])))
