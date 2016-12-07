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
        [["freelancer/" :user/id] :freelancer/detail]
        [["employer/" :user/id] :employer/detail]
        [["job/" :job/id] :job/detail]
        [["job-proposal/" :contract/id] :contract/detail]
        [["contract/" :contract/id "/invoices"] :contract/invoices]
        [["invoice/" :invoice/id] :invoice/detail]
        [true :home]]])
