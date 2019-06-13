(ns ethlance.shared.routes)


(def routes [["/" :route/home]

             ;; Users
             ["/arbiters" :route.user/arbiters]
             ["/candidates" :route.user/candidates]
             ["/employers" :route.user/employers]
             ["/user/" :route.user/employers]
             ["/user/:address" :route.user/profile]

             ;; Jobs
             ["/jobs" :route.job/jobs]
             ["/jobs/new" :route.job/new] ;; general & bounty
             ["/jobs/contract/" :route.job/contract]
             ["/jobs/contract/:index" :route.job/contract]
             ["/jobs/detail/" :route.job/detail]
             ["/jobs/detail/:index" :route.job/detail]
             
             ;; Me
             ["/me" :route.me/index]
             ["/me/sign-up" :route.me/sign-up]
             ["/me/contracts" :route.me/contracts]
             ["/me/contracts/:address/new-invoice" :route.me/new-invoice]])



(def dev-routes
  (-> routes
      (concat [["/devcard/index" :route.devcard/index]])
      vec))
