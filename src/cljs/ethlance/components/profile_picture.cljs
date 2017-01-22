(ns ethlance.components.profile-picture
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a]]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    ))

(defn profile-picture [{:keys [:user :employer? :size :hide-name?]}]
  (let [{:keys [:user/gravatar :user/name :user/id]} user]
    [a
     {:route-params {:user/id id}
      :route (if employer? :employer/detail :freelancer/detail)
      :style {:color :inherit}
      }
     [row
      {:center "xs"}
      [col {:xs 12}
       [ui/avatar
        {:size (or size 80)
         :src (u/gravatar-url gravatar id)}]]
      (when-not hide-name?
        [col {:xs 12
              :style styles/profile-picture-name}
         name])]]))
