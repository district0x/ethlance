(defproject ethlance "0.1.0-SNAPSHOT"
  :dependencies [[akiroz.re-frame/storage "0.1.1"]
                 [bidi "2.0.14"]
                 [camel-snake-kebab "0.4.0"]
                 [cljs-ajax "0.5.8"]
                 [cljs-react-material-ui "0.2.38"]
                 [cljs-web3 "0.18.2-0"]
                 [cljsjs/bignumber "2.1.4-1"]
                 [cljsjs/linkify "2.1.4-0" :exclusions [cljsjs/react]]
                 [cljsjs/material-ui-chip-input "0.13.0-0"]
                 [cljsjs/oauthio "0.6.1-0"]
                 [cljsjs/react-flexbox-grid "0.10.2-1" :exclusions [cljsjs/react cljsjs/react-dom]]
                 [cljsjs/react-highlight "1.0.5-0" :exclusions [cljsjs/react cljsjs/react-dom]]
                 [cljsjs/react-truncate "2.0.3-0"]
                 [cljsjs/react-ultimate-pagination "0.8.0-0" :exclusions [cljsjs/react cljsjs/react-dom]]
                 [com.andrewmcveigh/cljs-time "0.4.0"]
                 [com.cemerick/url "0.1.1"]
                 [day8.re-frame/async-flow-fx "0.0.6"]
                 [day8.re-frame/http-fx "0.0.4"]
                 [kibu/pushy "0.3.6"]
                 [madvas.re-frame/google-analytics-fx "0.1.0"]
                 [madvas.re-frame/web3-fx "0.1.4"]
                 [medley "0.8.3"]
                 [org.clojure/clojure "1.9.0-alpha10"]
                 [org.clojure/clojurescript "1.9.293"]
                 [print-foo-cljs "2.0.3"]
                 [re-frame "0.9.1"]
                 [reagent "0.6.1p-SNAPSHOT" :exclusions [cljsjs/react cljsjs/react-dom]]]

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

  :aliases {"compile-solidity" ["shell" "./compile-solidity.sh"]}

  :less {:source-paths ["resources/public/less"]
         :target-path "resources/public/css"
         :target-dir "resources/public/css"
         :source-map true
         :compression true}

  :profiles
  {:dev
   {:dependencies [[binaryage/devtools "0.8.3"]
                   [com.cemerick/piggieback "0.2.1"]
                   [figwheel-sidecar "0.5.8"]
                   [org.clojure/tools.nrepl "0.2.11"]]
    :plugins [[lein-figwheel "0.5.8"]]}}

  :cljsbuild
  {:builds
   [{:id "dev"
     :source-paths ["src/cljs"]
     :figwheel {:on-jsload "ethlance.core/mount-root"}
     :compiler {:main ethlance.core
                :output-to "resources/public/js/compiled/app.js"
                :output-dir "resources/public/js/compiled/out"
                :asset-path "js/compiled/out"
                :source-map-timestamp true
                :preloads [print.foo.preloads.devtools]
                :closure-defines {goog.DEBUG true}
                :external-config {:devtools/config {:features-to-install :all}}
                }}

    {:id "min"
     :source-paths ["src/cljs"]
     :compiler {:main ethlance.core
                :output-to "resources/public/js/compiled/app.js"
                :optimizations :advanced
                :closure-defines {goog.DEBUG false}
                :pretty-print false
                :externs ["src/js/externs.js"]}}]})
