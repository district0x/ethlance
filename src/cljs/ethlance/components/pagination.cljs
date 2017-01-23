(ns ethlance.components.pagination
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.icons :as icons]
    [cljsjs.react-ultimate-pagination]
    [ethlance.styles :as styles]
    [reagent.core :as r]))

(def config
  {:itemTypeToComponent
   {js/ReactUltimatePagination.ITEM_TYPES.PAGE
    (fn [props]
      (let [{:keys [value isActive onClick]} (js->clj props :keywordize-keys true)]
        (r/as-element
          [ui/flat-button
           {:style styles/pagination-button
            :label (str value)
            :secondary isActive
            :on-touch-tap onClick}])))

    js/ReactUltimatePagination.ITEM_TYPES.ELLIPSIS
    (fn [props]
      (r/as-element
        [ui/flat-button {:style styles/pagination-button :label "..." :on-touch-tap (aget props "onClick")}]))

    js/ReactUltimatePagination.ITEM_TYPES.FIRST_PAGE_LINK
    (fn [props]
      (let [{:keys [isActive onClick]} (js->clj props :keywordize-keys true)]
        (r/as-element
          [ui/flat-button
           {:style styles/pagination-button
            :icon (icons/page-first)
            :on-touch-tap onClick
            :disabled isActive}])))

    js/ReactUltimatePagination.ITEM_TYPES.PREVIOS_PAGE_LINK
    (fn [props]
      (let [{:keys [isActive onClick]} (js->clj props :keywordize-keys true)]
        (r/as-element
          [ui/flat-button
           {:style styles/pagination-button
            :icon (icons/chevron-left)
            :on-touch-tap onClick
            :disabled isActive}])))

    js/ReactUltimatePagination.ITEM_TYPES.NEXT_PAGE_LINK
    (fn [props]
      (let [{:keys [isActive onClick]} (js->clj props :keywordize-keys true)]
        (r/as-element
          [ui/flat-button
           {:style styles/pagination-button
            :icon (icons/chevron-right)
            :on-touch-tap onClick
            :disabled isActive}])))

    js/ReactUltimatePagination.ITEM_TYPES.LAST_PAGE_LINK
    (fn [props]
      (let [{:keys [isActive onClick]} (js->clj props :keywordize-keys true)]
        (r/as-element
          [ui/flat-button
           {:style styles/pagination-button
            :icon (icons/page-last)
            :on-touch-tap onClick
            :disabled isActive}])))}})

(def pagination (r/adapt-react-class (js/ReactUltimatePagination.createUltimatePagination (clj->js config))))