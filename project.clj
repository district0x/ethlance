(defproject district0x/ethlance "2.0.0-SNAPSHOT"
  :url "https://github.com/district0x/ethlance"
  :dependencies [[camel-snake-kebab "0.4.1"]
                 [cljs-web3-next "0.1.3"]
                 [cljsjs/bignumber "4.1.0-0"]
                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 [com.rpl/specter "1.1.3"]
                 [com.taoensso/encore "2.120.0"]
                 [com.taoensso/timbre "4.10.0"]
                 [district0x/async-helpers "0.1.3"]
                 [district0x/bignumber "1.0.3"]
                 [district0x/cljs-ipfs-http-client "1.0.5"]
                 [district0x/district-cljs-utils "1.0.4"]
                 [district0x/district-encryption "1.0.1"]
                 [district0x/district-format "1.0.8"]
                 [district0x/district-graphql-utils "1.0.10"]
                 [district0x/district-parsers "1.0.0"]
                 [district0x/district-sendgrid "1.0.1"]
                 [district0x/district-server-config "1.0.1"]
                 [district0x/district-server-db "1.0.4"]
                 [district0x/district-server-logging "1.0.6"]
                 [district0x/district-server-middleware-logging "1.0.0"]
                 [district0x/district-server-smart-contracts "1.2.5"]
                 [district0x/district-server-web3 "1.2.6"]
                 [district0x/district-server-web3-events "1.1.10"]
                 [district0x/district-time "1.0.1"]
                 [district0x/error-handling "1.0.4"]
                 [district0x/graphql-query "1.0.6"]
                 [district0x/honeysql "1.0.444"]
                 [district0x/mount "0.1.17"]
                 [expound "0.8.4"]
                 [funcool/cuerdas "2.2.0"]
                 [medley "1.3.0"]
                 [orchestra "2019.02.06-1"]
                 [org.clojars.mmb90/cljs-cache "0.1.4"]
                 [org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.773"]
                 [org.clojure/core.async "1.2.603"]
                 [org.clojure/core.match "1.0.0"]
                 [org.clojure/tools.reader "1.3.2"]]

  :plugins [[lein-ancient "0.6.15"]
            [lein-cljsbuild "1.1.8"]
            [lein-shell "0.5.0"]
            [lein-marginalia "0.9.1"]]

  :min-lein-version "2.5.3"
  :source-paths ["src"]
  :test-paths ["test"]
  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target" "dist"]
  :figwheel {:css-dirs ["resources/public/css"]
             :nrepl-port 9000
             :server-ip "127.0.0.1"
             :server-port 6500
             :ring-handler handler/figwheel-request-handler}
  :exclusions [cljsjs/react-with-addons
               honeysql
               reagent
               mount
               district0x/district-ui-router]
  :profiles
  {:dev
   {:source-paths ["src" "test" "dev"]
    :resource-paths ["resources" "dev/resources"]
    :dependencies [[binaryage/devtools "1.0.1"]
                   [cider/piggieback "0.5.0"]
                   [doo "0.1.11"]
                   [figwheel "0.5.20"]
                   [figwheel-sidecar "0.5.20"]
                   [org.clojure/tools.nrepl "0.2.13"]
                   [re-frisk "1.3.4"]]
    :plugins [[lein-figwheel "0.5.20"]
              [lein-doo "0.1.10"]]
    :repl-options {:init-ns user
                   :nrepl-middleware [cider.piggieback/wrap-cljs-repl]}}

   :dev-ui
   {:figwheel {:nrepl-port 9000
               :server-ip "127.0.0.1"
               :server-port 6500}}

   :dev-server
   {:figwheel {:nrepl-port 9001
               :server-ip "localhost"
               :server-port 6501}}}

  :cljsbuild
  {:builds
   [{:id "dev-server"
     :source-paths ["src/ethlance/server" "src/ethlance/shared" "dev/server/cljs" "src/district/server"]
     :figwheel {:on-jsload "ethlance.server.graphql.server/restart"}
     :compiler {:main ethlance.server.core
                :output-to "target/node/ethlance_server.js"
                :output-dir "target/node/out-dev-server"
                :target :nodejs
                :optimizations :none
                :source-map true
                :source-map-timestamp true
                :closure-defines {goog.DEBUG true}}}
    {:id "prod-server"
     :source-paths ["src"]
     :compiler {:main ethlance.server.core
                :output-to "dist/ethlance_server.js"
                :output-dir "target/node/out-prod-server"
                :target :nodejs
                :optimizations :simple
                :closure-defines {goog.DEBUG false}
                :pretty-print false}}
    {:id "test-server"
     :source-paths ["src/ethlance/server" "test/ethlance/server" "src/ethlance/shared"]
     :compiler {:main ethlance.server.test-runner
                :output-to "target/node_test/test_runner.js"
                :output-dir "target/node_test/out-server-test-runner"
                :target :nodejs
                :source-map-timestamp true
                :closure-defines {goog.DEBUG true}}}]})
