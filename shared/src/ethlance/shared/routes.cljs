(ns ethlance.shared.routes)


(def routes [["/" :route/home]

             ;; Users
             ["/arbiters" :route.user/arbiters]
             ["/candidates" :route.user/candidates]
             ["/employers" :route.user/employers]
             ["/user/" :route.user/profile]
             ["/user/:address" :route.user/profile]

             ;; Jobs
             ["/jobs" :route.job/jobs]
             ["/jobs/new" :route.job/new]
             ["/jobs/contract/" :route.job/contract]
             ["/jobs/contract/:job-story-id" :route.job/contract]
             ["/jobs/detail" :route.job/detail] ; TODO: possibly to be used to poll & resolve to the last job created (if the address was unknown)
             ["/jobs/detail/:id" :route.job/detail]

             ;; Invoices
             ["/invoices/new" :route.invoice/new]
             ["/invoices/" :route.invoice/index]
             ["/invoices/:id" :route.invoice/index]

             ;; Me
             ["/me" :route.me/index]
             ["/me/sign-up" :route.me/sign-up]

             ;; Misc.
             ["/how-it-works" :route.misc/how-it-works]
             ["/about" :route.misc/about]])


(def dev-routes
  (conj routes ["/devcard/index" :route.devcard/index]))
