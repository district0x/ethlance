(defproject ethlance "0.1.0-SNAPSHOT"
  :dependencies [[akiroz.re-frame/storage "0.1.2"]
                 [bidi "2.0.14"]
                 [cljs-ajax "0.5.8"]
                 [cljs-react-material-ui "0.2.46"]
                 [cljsjs/bignumber "2.1.4-1"]
                 [cljsjs/linkify "2.1.4-0" :exclusions [cljsjs/react]]
                 [cljsjs/material-ui-chip-input "0.15.0-0"]
                 [cljsjs/oauthio "0.6.1-0"]
                 [cljsjs/react-flexbox-grid "1.0.0-0" :exclusions [cljsjs/react cljsjs/react-dom]]
                 [cljsjs/react-highlight "1.0.5-0" :exclusions [cljsjs/react cljsjs/react-dom]]
                 [cljsjs/react-truncate "2.0.3-0"]
                 [cljsjs/react-ultimate-pagination "0.8.0-0" :exclusions [cljsjs/react cljsjs/react-dom]]
                 [cljsjs/solidity-sha3 "0.4.1-0"]
                 [cljsjs/web3 "0.18.4-0"]
                 [com.andrewmcveigh/cljs-time "0.4.0"]
                 [com.cemerick/url "0.1.1"]
                 [day8.re-frame/async-flow-fx "0.0.6"]
                 [day8.re-frame/http-fx "0.1.3"]
                 [district0x/district-ui-mobile "1.0.0"]
                 [kibu/pushy "0.3.6"]
                 [madvas.re-frame/google-analytics-fx "0.1.0"]
                 [madvas.re-frame/web3-fx "0.1.12"]
                 [madvas/reagent-patched "0.6.1" :exclusions [cljsjs/react cljsjs/react-dom]]
                 [medley "0.8.3"]
                 [mount "0.1.13"]
                 [org.clojure/clojure "1.9.0-alpha10"]
                 [org.clojure/clojurescript "1.9.671"]
                 [print-foo-cljs "2.0.3"]
                 [re-frame "0.9.2" :exclusions [reagent]]]

  :plugins [[lein-auto "0.1.2"]
            [lein-cljsbuild "1.1.4"]
            [lein-shell "0.5.0"]
            [deraen/lein-less4j "0.5.0"]]

  :min-lein-version "2.5.3"

  :source-paths ["src/clj" "src/cljs"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :figwheel {:css-dirs ["resources/public/css"]
             :server-port 6229}

  :auto {"compile-solidity" {:file-pattern #"\.(sol)$"
                             :paths ["resources/public/contracts/src"]}}

  :aliases {"compile-solidity" ["shell" "./compile-solidity.sh"]
            "start-testrpc" ["shell" "./start-testrpc.sh"]}

  :less {:source-paths ["resources/public/less"]
         :target-path "resources/public/css"
         :target-dir "resources/public/css"
         :source-map true
         :compression true}

  :profiles
  {:dev
   {:source-paths ["src/clj" "dev"]
    :dependencies [[com.cemerick/piggieback "0.2.2"]
                   [figwheel "0.5.16"]
                   [figwheel-sidecar "0.5.16"]
                   [org.clojure/tools.nrepl "0.2.13"]
                   [binaryage/devtools "0.9.10"]]
    :plugins [[lein-figwheel "0.5.16"]]
    :repl-options {:init-ns ethlance.dev.user
                   :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]
                   :port 6230}}}

  :cljsbuild
  {:builds
   [{:id "dev"
     :source-paths ["src/cljs" "dev"]
     :figwheel {:on-jsload "ethlance.core/mount-root"}
     :compiler {:main ethlance.core
                :output-to "resources/public/js/compiled/app.js"
                :output-dir "resources/public/js/compiled/out"
                :asset-path "js/compiled/out"
                :source-map-timestamp true
                :preloads [print.foo.preloads.devtools]
                :closure-defines {goog.DEBUG true}
                :external-config {:devtools/config {:features-to-install :all}}}}

    {:id "min"
     :source-paths ["src/cljs"]
     :compiler {:main ethlance.core
                :output-to "resources/public/js/compiled/app.js"
                :optimizations :advanced
                :closure-defines {goog.DEBUG false}
                :pretty-print false
                :pseudo-names false
                :externs ["src/js/externs.js"]}}]})
