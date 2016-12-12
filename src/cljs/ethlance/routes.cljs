(ns ethlance.routes)

(def routes
  ["/" [["my-profile" :my-profile]
        ["become-freelancer" :freelancer/create]
        ["become-employer" :employer/create]
        #_["my-jobs/" {"freelancer" :freelancer/contracts
                       "employer" :employer/jobs}]
        #_["my-invoices/" {"freelancer" :freelancer/invoices
                           "employer" :employer/invoices}]
        ["search/" {"jobs" :search/jobs
                    "freelancers" :search/freelancers}]
        ["freelancer/" {"my-invoices" :freelancer/invoices
                        "my-contracts" :freelancer/contracts}]
        ["employer/" {"my-invoices" :employer/invoices
                      "my-jobs" :employer/jobs}]
        [["freelancer/" :user/id] :freelancer/detail]
        [["employer/" :user/id] :employer/detail]
        ["job/create" :job/create]
        [["job/" :job/id] :job/detail]
        [["job-proposal/" :contract/id] :contract/detail]
        [["contract/" :contract/id "/invoices"] :contract/invoices]
        ["invoice/create" :invoice/create]
        [["invoice/" :invoice/id] :invoice/detail]
        [true :home]]])
