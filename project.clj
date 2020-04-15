(defproject district0x/ethlance "2.0.0-SNAPSHOT"
  :url "https://github.com/district0x/ethlance"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.597"]

                 ;; General
                 [akiroz.re-frame/storage "0.1.4"]
                 [camel-snake-kebab "0.4.0"]
                 [cljs-web3-next "0.1.1"]
                 [cljsjs/buffer "5.1.0-1"]
                 [cljsjs/d3 "5.12.0-0"]
                 [cljsjs/react-infinite "0.13.0-0"]
                 [flib/simplebar "5.0.7-SNAPSHOT"]

                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 [com.rpl/specter "1.1.2"]
                 [com.taoensso/encore "2.116.0"]
                 [com.taoensso/timbre "4.10.0"]
                 [district0x/bignumber "1.0.3"]
                 [cljsjs/bignumber "4.1.0-0"]
                 [district0x/error-handling "1.0.4"]
                 [expound "0.7.2"]
                 [funcool/cuerdas "2.2.0"]
                 [garden "1.3.9"]
                 [medley "1.2.0"]
                 [mount "0.1.16"]
                 [orchestra "2019.02.06-1"]
                 [org.clojars.mmb90/cljs-cache "0.1.4"]
                 [org.clojure/core.async "0.4.500"]
                 [org.clojure/core.match "0.3.0"]
                 [org.clojure/tools.reader "1.3.2"]
                 [print-foo-cljs "2.0.3"]
                 [re-frame "0.11.0-rc2"]
                 [reagent "0.9.0-rc2"]

                 ;; District General Libraries
                 [district0x/async-helpers "0.1.3"]
                 [district0x/district-cljs-utils "1.0.4"]
                 [district0x/district-encryption "1.0.1"]
                 [district0x/district-format "1.0.7"]
                 [district0x/district-graphql-utils "1.0.10"]
                 [district0x/district-sendgrid "1.0.1"]
                 [district0x/district-time "1.0.1"]
                 [district0x/graphql-query "1.0.6"]

                 ;; District Server Components
                 [district0x/district-server-config "1.0.1"]
                 [district0x/district-server-db "1.0.4"]
                 [district0x/district-server-logging "1.0.5"]
                 [district0x/district-server-middleware-logging "1.0.0"]
                 [district0x/district-server-smart-contracts "1.2.5"]
                 [district0x/district-server-web3 "1.2.3"]
                 [district0x/district-server-web3-events "1.1.10"]

                 ;; UI Components
                 [cljs-web3 "0.19.0-0-10"]
                 ;; this is now cljs-web3.utils/solidity-sha3
                 [district0x/cljs-solidity-sha3 "1.0.0"]
                 ;; this is now cljs-web3.helpers
                 [district0x/district-web3-utils "1.0.3"]

                 ;; District UI Components
                 [day8.re-frame/http-fx "0.1.6"]
                 [district0x/cljs-ipfs-http-client "1.0.5"]
                 [district0x/district-ui-component-active-account "1.0.1"]
                 [district0x/district-ui-component-active-account-balance "1.0.1"]
                 [district0x/district-ui-component-form "0.2.13"]
                 [district0x/district-ui-component-input "1.0.0"]
                 [district0x/district-ui-component-notification "1.0.0"]
                 [district0x/district-ui-component-tx-button "1.0.0"]
                 [district0x/district-ui-graphql "1.0.10"]
                 [district0x/district-ui-logging "1.1.0"]
                 [district0x/district-ui-notification "1.0.1"]
                 [district0x/district-ui-now "1.0.2"]
                 [district0x/district-ui-reagent-render "1.0.1"]
                 [district0x/district-ui-router "1.0.5"]
                 [district0x/district-ui-router-google-analytics "1.0.1"]
                 [district0x/district-ui-smart-contracts "1.0.8"]
                 [district0x/district-ui-web3 "1.3.2"]
                 [district0x/district-ui-web3-account-balances "1.0.2"]
                 [district0x/district-ui-web3-accounts "1.0.6"]
                 [district0x/district-ui-web3-balances "1.0.2"]
                 [district0x/district-ui-web3-sync-now "1.0.3-2"]
                 [district0x/district-ui-web3-tx "1.0.11"]
                 [district0x/district-ui-web3-tx-id "1.0.1"]
                 [district0x/district-ui-web3-tx-log "1.0.12"]
                 [district0x/district-ui-window-size "1.0.1"]
                 [district0x/re-frame-ipfs-fx "1.1.1"]]

  :plugins [[lein-ancient "0.6.15"]
            [lein-cljsbuild "1.1.7"]
            [lein-npm "0.6.2"]
            [lein-shell "0.5.0"]
            [lein-marginalia "0.9.1"]]

  :min-lein-version "2.5.3"
  :source-paths ["src"]
  :test-paths ["test"]
  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target" "dist"]
  :figwheel {:css-dirs ["resources/public/css"]
             :nrepl-port 9000
             :server-ip "0.0.0.0"
             :server-port 6500
             :ring-handler handler/figwheel-request-handler}
  :exclusions [cljsjs/react-with-addons
               reagent]
  :npm {:dependencies
        [["@sentry/node" "4.2.1"]

         [apollo-server "2.9.13"]
         [graphql-middleware "4.0.1"]
         [graphql-tools "4.0.5"]
         [graphql "14.2.1"]

         [better-sqlite3 "5.4.0"]
         [chalk "2.3.0"]
         [cors "2.8.4"]
         [source-map-support "0.5.9"]
         [ws "4.0.0"]

         ;; Sign in functionality
         [eth-sig-util "2.4.4"]
         [jsonwebtoken "8.5.1"]
         ;; Note: district0x/district-server-web3 uses ganache-core@2.0.2, which depends on 0.6.0
         ;; Note: https://github.com/ethereumjs/ethereumjs-wallet/issues/64
         [ethereumjs-wallet "0.6.0"]
         [jsedn "0.4.1"]

         ;; Development Dependencies
         [axios "0.19.0"]
         [webpack "4.41.2"]
         [webpack-cli "3.3.9"]
         [less "3.10.3"]
         [less-watch-compiler "1.14.1"]
         [ganache-cli "6.7.0"]
         [truffle "5.0.38"]]}

  :profiles
  {:dev
   {:source-paths ["src" "test" "dev"]
    :resource-paths ["resources" "dev/resources"]
    :dependencies [[cider/piggieback "0.4.1"]
                   [org.clojure/tools.nrepl "0.2.13"]
                   [figwheel "0.5.19"]
                   [figwheel-sidecar "0.5.19"]
                   [binaryage/devtools "0.9.10"]
                   [doo "0.1.11"]]
    :plugins [[lein-figwheel "0.5.19"]
              [lein-doo "0.1.10"]]
    :repl-options {:init-ns user
                   :nrepl-middleware [cider.piggieback/wrap-cljs-repl]}}}

  :cljsbuild
  {:builds
   [{:id "dev-ui"
     :source-paths ["src/ethlance/ui" "src/ethlance/shared" "dev/ui"]
     :figwheel {:websocket-host :js-client-host
                :on-jsload "district.ui.reagent-render/rerender"}
     :compiler {:main ethlance.ui.core
                :infer-externs true
                :npm-deps false
                :output-to "resources/public/js/compiled/ethlance_ui.js"
                :output-dir "resources/public/js/compiled/out-dev-ui"
                :asset-path "/js/compiled/out-dev-ui"
                :optimizations :none
                :source-map true
                :source-map-timestamp true
                :closure-defines {goog.DEBUG true}}}

    {:id "dev-server"
     :source-paths ["src/ethlance/server" "src/ethlance/shared" "dev/server/cljs"]
     :figwheel {:on-jsload "ethlance.server.graphql.server/restart"}
     :compiler {:main ethlance.server.core
                :output-to "target/node/ethlance_server.js"
                :output-dir "target/node/out-dev-server"
                :target :nodejs
                :optimizations :none
                :source-map true
                :source-map-timestamp true
                :closure-defines {goog.DEBUG true}}}

    {:id "prod-ui"
     :source-paths ["src"]
     :compiler {:main ethlance.ui.core
                :output-to "dist/resources/public/js/compiled/ethlance_ui.min.js"
                :output-dir "dist/resources/public/js/compiled/out-prod-ui"
                :optimizations :advanced
                :closure-defines {goog.DEBUG false}
                :pretty-print false}}

    {:id "prod-server"
     :source-paths ["src"]
     :compiler {:main ethlance.server.core
                :output-to "dist/ethlance_server.js"
                :output-dir "target/node/out-prod-server"
                :target :nodejs
                :optimizations :simple
                :closure-defines {goog.DEBUG false}
                :pretty-print false}}

    {:id "test-ui"
     :source-paths ["src" "test" "dev/ui"]
     :compiler {:main ethlance.ui.test-runner ;; ./test/ui
                :output-to "dev/resources/public/js/compiled/test_runner.js"
                :output-dir "dev/resources/public/js/compiled/out-ui-test-runner"
                :asset-path "/js/compiled/out-ui-test-runner"
                :source-map-timestamp true
                :closure-defines {goog.DEBUG true}}}

    {:id "test-server"
     :source-paths ["src" "test" "dev/server"]
     :compiler {:main ethlance.server.test-runner
                :output-to "target/node_test/test_runner.js"
                :output-dir "target/node_test/out-server-test-runner"
                :target :nodejs
                :source-map-timestamp true
                :closure-defines {goog.DEBUG true}}}]})
