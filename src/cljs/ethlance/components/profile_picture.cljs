(ns ethlance.components.profile-picture
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a]]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    ))

(defn profile-picture [{:keys [user employer?]}]
  (let [{:keys [:user/gravatar :user/name :user/id]} user]
    [a
     {:route-params {:user/id id}
      :route (if employer? :employer/detail :freelancer/detail)
      :style styles/fade-text}
     [row
      {:center "xs"}
      [col {:xs 12}
       [ui/avatar
        {:size 80
         :src (u/gravatar-url gravatar)}]]
      [col {:xs 12
            :style (merge styles/overflow-ellipsis
                          styles/text-center)}
       name]]]))
