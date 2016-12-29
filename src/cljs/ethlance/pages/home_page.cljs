(ns ethlance.pages.home-page
  (:require
    [cljs-react-material-ui.icons :as icons]
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a]]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn landing-menu []
  [row
   {:center "xs" :bottom "xs"
    :style {:height "100%"}}
   [ui/paper {:style styles/landing-menu}
    [row
     {:between "xs" :middle "xs"}
     [col {:xs 4}
      [row-plain
       {:middle "xs" :around "xs"}
       [:img {:style styles/landing-menu-icon-smaller
              :src "./images/zero-percent.svg"}]
       [:h3 {:style styles/landing-menu-title} "0% service fees"]]]
     [col {:xs 4}
      [row-plain
       {:middle "xs" :around "xs"}
       [:img {:style styles/landing-menu-icon
              :src "./images/decentralised.svg"}]
       [:h3 {:style styles/landing-menu-title} "Fully decentralised" [:br] "on blockchain"]]]
     [col {:xs 4}
      [row-plain
       {:middle "xs" :around "xs"}
       [:img {:style styles/landing-menu-icon-smaller
              :src "./images/free-tag2.svg"}]
       [:h3 {:style styles/landing-menu-title} "No restrictions" [:br] "Free membership"]]]]]])

(defn feature-no-cut []
  [row
   {:middle "xs" :center "xs"
    :style {:background-color "#bbdefb"}}
   [col
    {:xs 5
     :style styles/text-left}
    [:h1.black "We take no cut!"]
    [:h3.black "Ethlance doesn’t take any percentage of your earned Ether. Amount of Ether employer pays, equals exactly to what freelancer gets."]]
   [col
    {:xs 3}
    [:img
     {:src "./images/coins-cloud.svg"
      :style {:max-height "600px"}}]]])

(defn feature-blockchain []
  [row
   {:middle "xs" :center "xs"
    :style {:padding-bottom "50px"
            :background-color "#FFF"
            :height "604px"}}
   [col
    {:xs 3}
    [:img
     {:src "./images/ethereum.png"
      :style styles/ethereum-logo-landing}]]
   [col
    {:xs 5
     :style styles/text-left}
    [:h1.black "It’s all on blockchain!"]
    [:h3.black "Ethlance database is distributed on Ethereum public blockchain. This makes data accessible to everyone forever, without any central authority having control over it."]]])

(defn feature-no-restrictions []
  [row
   {:middle "xs" :center "xs"
    :style {:background-color "#ff8a80"
            :height "604px"}}
   [col
    {:xs 4
     :style styles/text-left}
    [:h1.black "No artificial restrictions!"]
    [:h3.black "Everybody can apply for, or create unlimited number of jobs. All that is needed, is to pay Ethereum gas fees associated with these operations."]]
   [col
    {:xs 5}
    [:img
     {:src "./images/to-the-top.svg"
      :style {:max-height "400px"}}]]])

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
  [:div
   {:style {:background-color "#FFF"
            :padding "36px 0"}}
   [:h1.black {:style styles/text-center} "How it works?"]
   [row
    {:center "xs"}
    (for [[title path] [["Become Freelancer" freelancer-path] ["Become Employer" employer-path]]]
      [col
       {:xs 3 :key (ffirst path)}
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
                    :src "../images/arrow.svg"}])])]])]])

(defn footer-link [route text]
  [:div
   [:a {:href (u/path-for route)
        :style styles/white-text}
    text]])

(defn footer []
  [row
   {:middle "xs" :around "xs"
    :style {:background-color "#181a1a"
            :color "#FFF"
            :height "200px"}}
   [col
    {:xs 2 :xs-offset 2}
    [ui/subheader {:style styles/footer-subheader}
     "LEARN MORE"]
    [footer-link :about "About us"]
    [footer-link :about "How it works?"]]
   [col
    {:xs 2}
    [ui/subheader {:style styles/footer-subheader}
     "GET STARTED"]
    [footer-link :freelancer/create "Become Freelancer"]
    [footer-link :employer/create "Become Employer"]
    [footer-link :search/jobs "Find Work"]
    [footer-link :search/freelancers "Find Freelancers"]]
   [col
    {:xs 3}]])

(defn home-page []
  [row
   {:style {:height "100%"
            :background-color "#FFF"}}
   [col
    {:xs 12
     :style {:height "90%"}}
    [:div {:style styles/landing-bg}
     [landing-menu]]]
   [col
    {:xs 12}
    [feature-blockchain]]
   [col
    {:xs 12}
    [feature-no-cut]]
   [col
    {:xs 12}
    [feature-no-restrictions]]
   [col
    {:xs 12}
    [process-diagram]]
   [col
    {:xs 12}
    [footer]]])
