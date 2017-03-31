(ns ethlance.pages.sponsor-detail-page
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [clojure.set :as set]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a center-layout currency]]
    [ethlance.components.user-sponsorships :refer [user-sponsorships]]
    [ethlance.constants :as constants]
    [ethlance.ethlance-db :as ethlance-db]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn sponsor-detail-page []
  [center-layout
   [user-sponsorships
    {:user/id @(subscribe [:user/route-user-id])}]])