(ns soda-ash.core
  (:require
    [cljsjs.semantic-ui-react]
    [reagent.core])
  (:require-macros
    [soda-ash.macros :refer [export-semantic-ui-react-components]]))


;; Turned off soda-ash because
;;   1) it didn't seem to be used in any crucial places
;;   2) It causes errors
;; (export-semantic-ui-react-components)
