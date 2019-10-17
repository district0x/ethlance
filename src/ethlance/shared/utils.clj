(ns ethlance.shared.utils)

(defmacro get-environment []
  (let [env (or (System/getenv "ETHLANCE_ENV") "dev")]
    ;; Write to stderr instead of stdout because the cljs compiler
    ;; writes stdout to the raw JS file.
    (binding [*out* *err*]
      (println "Building with environment:" env))
    env))
