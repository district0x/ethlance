(ns ethlance.ui.page.how-it-works
  (:require
    [district.ui.component.page :refer [page]]
    [ethlance.ui.component.main-layout :refer [c-main-layout]]
    [ethlance.ui.util.content-helpers :refer [page-with-title]]))


(def description
  (str
      "The free market is an economic system where the prices of goods and services are determined by unrestricted competition between privately owned businesses. In this system, supply and demand dictate production decisions and pricing, allowing consumers and producers to engage freely without significant government intervention. Businesses respond to consumer preferences, leading to an efficient allocation of resources as they strive to meet market needs.
      Competition is a cornerstone of the free market, driving innovation and quality improvement. Companies compete to offer better products and services at lower prices to attract consumers. This competitive environment encourages efficiency and technological advancements, which can lead to overall economic growth and increased standards of living. Entrepreneurs have the freedom to enter markets, fostering diversity and choice for consumers.
      However, for a free market to function effectively, certain legal frameworks must be in place, such as property rights and contract enforcement. While the free market promotes efficiency and innovation, it can also result in challenges like income inequality and market failures if left entirely unchecked. Therefore, some level of regulation is often considered necessary to address issues like monopolies, externalities, and to protect consumers and workers, balancing freedom with societal welfare."))


(defmethod page :route.misc/how-it-works []
  (fn []
    [c-main-layout {:container-opts {:class :how-it-works-main-container}}
     (page-with-title "How It Works" description)]))
