(ns ethlance.server.tracing.api
  (:require
    ["@opentelemetry/api" :refer [trace context SpanStatusCode ROOT_CONTEXT]]))

(def SPAN_STATUS_OK (.-OK SpanStatusCode))
(def SPAN_STATUS_ERROR (.-ERROR SpanStatusCode))

(defn set-span-context! [span]
  (.setSpan trace (.active context) span))

(defn active-context []
  (.active context))

(defn set-span-attributes!
  [span attributes]
  (doseq [[k v] attributes] (.setAttribute span k v))
  span)

(defonce tracer (.getTracer trace "ethl-dev" "0.0.1"))

(defn start-span
  ([span-name]
   (start-span span-name nil (active-context)))
  ([span-name attributes]
   (start-span span-name attributes (active-context)))
  ([span-name attributes context]
   (.startSpan tracer span-name (clj->js {"attributes" attributes}) context)))

(defn start-nested-span [parent span-name & [attributes]]
  (let [ctx (set-span-context! parent)
        span (start-span span-name attributes ctx)]
    (set-span-attributes! span attributes)))

(defn end-span! [span]
  (.end span))

(defn add-event! [span event-name k-v-map]
  (.addEvent span event-name (clj->js k-v-map)))

(defn start-active-span [span-name callback]
  (.startActiveSpan tracer span-name callback))

(defn with-active-span [span-name callback]
  (-> (start-active-span span-name callback)
      (end-span! ,,,)))

(defn get-active-span
  ([]
   (.getActiveSpan trace)))

(defn set-span-error!
  [span error & [message]]
  (doto span
    (.setStatus ,,, (clj->js {"code" SPAN_STATUS_ERROR "message" message}))
    (.recordException ,,, error)))

(defn set-span-ok!
  [span]
  (.setStatus span (clj->js {"code" SPAN_STATUS_OK})))

(defn with-context
  [provided-context fn-to-call]
  (.with context provided-context fn-to-call js/undefined))

(defn with-span-context [span fn-to-call]
  (with-context (set-span-context! span) fn-to-call))
