(ns ethlance.routes)

(def routes
  ["/" [["my-profile" :my-profile]
        ["become-freelancer" :freelancer/create]
        ["become-employer" :employer/create]
        ["my-jobs/" {"freelancer" :freelancer/jobs
                     "employer" :employer/jobs}]
        ["my-invoices/" {"freelancer" :freelancer/invoices
                         "employer" :employer/invoices}]
        ["search/" {"jobs" :search/jobs
                    "freelancers" :search/freelancers}]
        [["freelancer/" :address] :freelancer/detail]
        [["employer/" :address] :employer/detail]
        [["job/" :job/id] :job/detail]
        [true :home]]])
