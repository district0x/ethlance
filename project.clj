(defproject district0x/ethlance "2.0.0-SNAPSHOT"
  :url "https://github.com/district0x/ethlance"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.339"]]

  :plugins [[lein-ancient "0.6.15"]
            [lein-solc "1.0.1-1"]
            [lein-cljsbuild "1.1.7"]
            [lein-npm "0.6.2"]
            [lein-shell "0.5.0"]]

  :min-lein-version "2.5.3"

  :source-paths ["src/clj" "src/ui" "src/server" "src/shared"]
  :test-paths ["test/clj"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target" "dist"]

  :figwheel {:css-dirs ["resources/public/css"]
             :nrepl-port 9000
             :server-port 6500}

  :aliases {}

  :exclusions [cljsjs/react-with-addons]

  :npm {:dependencies [[ws "4.0.0"]]}

  :solc {:src-path "resources/public/contracts/src"
         :build-path "resources/public/contracts/build"
         :solc-err-only true
         :verbose false
         :wc true
         :contracts :all}

  :profiles
  {:dev
   {:source-paths ["src/clj" "dev/clj"]
    ;; :resource-paths ["dev/resources"]
    :dependencies [[com.cemerick/piggieback "0.2.2"]
                   [figwheel "0.5.16"]
                   [figwheel-sidecar "0.5.16"]
                   [org.clojure/tools.nrepl "0.2.13"]
                   [binaryage/devtools "0.9.10"]]
    :plugins [[lein-figwheel "0.5.16"]
              [lein-doo "0.1.8"]]
    :repl-options {:init-ns ethlance.dev.user
                   :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]
                   :port 6499}}}

  :cljsbuild
  {:builds
   [{:id "dev-ui"
     :source-paths ["src/ui" "src/shared"
                    "dev/ui" "dev/shared"]
     :figwheel true
     :compiler {:main cljs.user ;; ./dev/ui
                :output-to "resources/public/js/compiled/ethlance_ui.js"
                :output-dir "resources/public/js/compiled/out-dev-ui"
                :asset-path "/js/compiled/out-dev-ui"
                :source-map-timestamp true
                :closure-defines {goog.DEBUG true}}}

    {:id "dev-server"
     :source-paths ["src/server" "src/shared"
                    "dev/server" "dev/shared"]
     :figwheel true
     :compiler {:main cljs.user ;; ./dev/server
                :output-to "target/node/ethlance_server.js"
                :output-dir "target/node/out-dev-server"
                :target :nodejs
                :source-map-timestamp true
                :closure-defines {goog.DEBUG true}}}

    {:id "prod-ui"
     :source-paths ["src/ui" "src/shared"]
     :compiler {:main ethlance.ui.core
                :output-to "dist/resources/public/js/compiled/ethlance_ui.min.js"
                :output-dir "dist/resources/public/js/compiled/out-prod-ui"
                :optimizations :advanced
                :closure-defines {goog.DEBUG false}
                :pretty-print false}}
    
    {:id "prod-server"
     :source-paths ["src/server" "src/shared"]
     :compiler {:main ethlance.server.core
                :output-to "dist/ethlance_server.min.js"
                :output-dir "target/node/out-prod-server"
                :target :nodejs
                :optimizations :simple
                :closure-defines {goog.DEBUG false}
                :pretty-print false}}
    
    {:id "test-ui"
     :source-paths ["src/ui" "src/shared"
                    "dev/ui" "dev/shared"
                    "test/ui" "test/shared"]
     :figwheel true
     :compiler {:main ethlance.ui.test-runner ;; ./test/ui
                :output-to "dev/resources/public/js/compiled/test_runner.js"
                :output-dir "dev/resources/public/js/compiled/out-ui-test-runner"
                :asset-path "/js/compiled/out-ui-test-runner"
                :source-map-timestamp true
                :closure-defines {goog.DEBUG true}}}
    
    {:id "test-server"
     :source-paths ["src/server" "src/shared"
                    "dev/server" "dev/shared"
                    "test/server" "test/shared"]
     :figwheel true
     :compiler {:main ethlance.server.test-runner ;; ./test/server
                :output-to "target/node_test/test_runner.js"
                :output-dir "target/node_test/out-server-test-runner"
                :target :nodejs
                :source-map-timestamp true
                :closure-defines {goog.DEBUG true}}}]})
