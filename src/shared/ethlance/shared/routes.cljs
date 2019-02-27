(ns ethlance.shared.routes)


(def routes [["/" :route/home]])


(def dev-routes
  (-> routes
      (concat [["/devcard/index" :route.devcard/index]])
      vec))
