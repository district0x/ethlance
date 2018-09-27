(defproject ethlance "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0-alpha10"]
                 [org.clojure/clojurescript "1.9.671"]]

  :plugins []

  :min-lein-version "2.5.3"

  :source-paths ["src/clj" "src/ui" "src/server"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :figwheel {:css-dirs ["resources/public/css"]
             :server-port 6229}

  :aliases {}

  :profiles
  {:dev
   {:source-paths ["src/clj" "dev/clj"]
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
   [{:id "dev-ui"
     :source-paths ["src/ui" "src/shared" "dev/ui" "dev/shared"]
     :figwheel {:on-jsload nil}
     :compiler {:main cljs.user ;; ./dev/ui
                :output-to "resources/public/js/compiled/ethlance_ui.js"
                :output-dir "resources/public/js/compiled/out"
                :asset-path "js/compiled/out"
                :source-map-timestamp true
                :closure-defines {goog.DEBUG true}}}

    {:id "dev-server"
     :source-paths ["src/server" "src/shared" "dev/server" "dev/shared"]
     :figwheel {:on-jsload nil}
     :compiler {:main cljs.user ;; ./dev/server
                :output-to "resources/public/js/compiled/ethlance_ui.js"
                :output-dir "resources/public/js/compiled/out"
                :asset-path "js/compiled/out"
                :source-map-timestamp true
                :closure-defines {goog.DEBUG true}}}

    {:id "prod-ui"
     :source-paths ["src/cljs"]
     :compiler {:main ethlance.ui.core
                :output-to "resources/public/js/compiled/ethlance_ui.min.js"
                :optimizations :advanced
                :closure-defines {goog.DEBUG false}
                :pretty-print false}}]})
    
    
    
