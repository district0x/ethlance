(ns ethlance.server.tracing.setup
  (:require
    ["@opentelemetry/sdk-node" :refer [NodeSDK]]
    ["@opentelemetry/sdk-trace-node" :refer [ConsoleSpanExporter]]
    ["@opentelemetry/exporter-trace-otlp-http" :refer [OTLPTraceExporter]]

    ["@opentelemetry/sdk-metrics" :refer [PeriodicExportingMetricReader
                                          ConsoleMetricExporter]]
    ["@opentelemetry/resources" :refer [Resource]]
    ["@opentelemetry/semantic-conventions" :refer [SEMRESATTRS_SERVICE_NAME
                                                   SEMRESATTRS_SERVICE_VERSION]]
    ["@opentelemetry/api" :refer [trace]]))


(def exporters
  {:OTLPTraceExporter (fn [config] (new OTLPTraceExporter (clj->js config)))
   :ConsoleSpanExporter (fn [& [_config]] (new ConsoleSpanExporter))})

(defn get-exporter [{:keys [name config]}]
  ((get exporters name) config))

(defn init-sdk [config]
  (let [service-name (get-in config [:sdk :name])
        service-version (get-in config [:sdk :version])
        resource (new Resource (clj->js {SEMRESATTRS_SERVICE_NAME service-name
                                         SEMRESATTRS_SERVICE_VERSION service-version}))
        trace-exporter (get-exporter (get-in config [:trace-exporter]))
        metric-exporter (new ConsoleMetricExporter)
        params {:resource resource
                :traceExporter trace-exporter
                :metricReader (new PeriodicExportingMetricReader (clj->js {:exporter metric-exporter}))}]
    (new NodeSDK (clj->js params))))

(def sdk (atom nil))

(defn start [config]
  (let [instance (init-sdk config)]
    (.start instance)
    (reset! sdk instance)))

(defn stop []
  (.shutdown @sdk))

;; It’s generally recommended to call getTracer in your app when you need it
;; rather than exporting the tracer instance to the rest of your app.
;; This helps avoid trickier application load issues when other required dependencies are involved.
(defn get-tracer [scope-name scope-version]
  (.getTracer trace scope-name scope-version))

(defonce tracer (get-tracer "syncer" "0.0.1"))
