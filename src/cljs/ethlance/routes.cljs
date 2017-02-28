(ns ethlance.routes)

(def routes
  ["/" [["how-it-works" :how-it-works]
        ["about" :about]
        ["edit-profile" :user/edit]
        ["become-freelancer" :freelancer/create]
        ["become-employer" :employer/create]
        ["find/" {"work" :search/jobs
                  "candidates" :search/freelancers}]
        ["search/" {"jobs" :search/jobs
                    "freelancers" :search/freelancers}]
        ["freelancer/" {"my-invoices" :freelancer/invoices
                        "my-contracts" :freelancer/contracts}]
        ["employer/" {"my-invoices" :employer/invoices
                      "my-jobs" :employer/jobs
                      "my-contracts" :employer/contracts}]
        [["freelancer/" :user/id] :freelancer/detail]
        [["employer/" :user/id] :employer/detail]
        ["job/create" :job/create]
        [["job/" :job/id] :job/detail]
        [["job-proposal/" :contract/id] :contract/detail]
        [["contract/" :contract/id "/invoices"] :contract/invoices]
        ["invoice/create" :invoice/create]
        [["invoice/" :invoice/id] :invoice/detail]
        [true :home]]])
