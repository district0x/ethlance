(ns ethlance.components.pagination
  (:require
    [cljs-react-material-ui.core :as ui]
    [cljs-react-material-ui.icons :as icons]
    [cljsjs.react-ultimate-pagination]
    [ethlance.styles :as styles]
    [reagent.core :as r]))

(def config
  {:itemTypeToComponent
   {js/ReactUltimatePagination.ITEM_TYPES.PAGE
    (fn [props]
      (let [{:keys [value isActive onClick]} (js->clj props :keywordize-keys true)]
        (ui/flat-button
          {:style styles/pagination-button
           :label (str value)
           :secondary isActive
           :on-touch-tap onClick})))

    js/ReactUltimatePagination.ITEM_TYPES.ELLIPSIS
    (fn [props]
      (ui/flat-button {:style styles/pagination-button :label "..." :on-touch-tap (aget props "onClick")}))

    js/ReactUltimatePagination.ITEM_TYPES.FIRST_PAGE_LINK
    (fn [props]
      (let [{:keys [isActive onClick]} (js->clj props :keywordize-keys true)]
        (ui/flat-button
          {:style styles/pagination-button
           :icon (icons/navigation-first-page)
           :on-touch-tap onClick
           :disabled isActive})))

    js/ReactUltimatePagination.ITEM_TYPES.PREVIOS_PAGE_LINK
    (fn [props]
      (let [{:keys [isActive onClick]} (js->clj props :keywordize-keys true)]
        (ui/flat-button
          {:style styles/pagination-button
           :icon (icons/navigation-chevron-left)
           :on-touch-tap onClick
           :disabled isActive})))

    js/ReactUltimatePagination.ITEM_TYPES.NEXT_PAGE_LINK
    (fn [props]
      (let [{:keys [isActive onClick]} (js->clj props :keywordize-keys true)]
        (ui/flat-button
          {:style styles/pagination-button
           :icon (icons/navigation-chevron-right)
           :on-touch-tap onClick
           :disabled isActive})))

    js/ReactUltimatePagination.ITEM_TYPES.LAST_PAGE_LINK
    (fn [props]
      (let [{:keys [isActive onClick]} (js->clj props :keywordize-keys true)]
        (ui/flat-button
          {:style styles/pagination-button
           :icon (icons/navigation-last-page)
           :on-touch-tap onClick
           :disabled isActive})))}})

(def pagination (r/adapt-react-class (js/ReactUltimatePagination.createUltimatePagination (clj->js config))))