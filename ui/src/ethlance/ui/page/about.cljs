(ns ethlance.ui.page.about
  (:require
    [district.ui.component.page :refer [page]]
    [ethlance.ui.component.main-layout :refer [c-main-layout]]
    [ethlance.ui.util.content-helpers :refer [page-with-title]]))


(def description
  "The district0x network is a collective of decentralized marketplaces and communities known as ‘Districts’. Districts exist on top of a modular framework of Ethereum smart contracts and frontend libraries referred to as d0xINFRA.
  The district0x network solves a number of coordination issues and inefficiencies commonly found within distributed community marketplaces. This is accomplished by providing tools that can better align incentives and decision making among the market participants themselves. The end goal is to create a self sustaining ecosystem that can flourish without the need for a central authority.")

(defmethod page :route.misc/about []
  (fn []
    [c-main-layout {:container-opts {:class :about-main-container}}
     (page-with-title "About" description)]))
