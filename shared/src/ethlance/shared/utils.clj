(ns ethlance.shared.utils
  (:refer-clojure :exclude [slurp]))


(defmacro get-environment
  "Gets the environment variable ETHLANCE_ENV value *during build time*
  Because macros are built in JVM process. The value of get-environment
  will be 'hardcoded' or fixed after the build, i.e. will not take into regard
  the value changes when running the compiled code after the build"
  []
  (let [env (or (System/getenv "ETHLANCE_ENV") "dev")]
    ;; Write to stderr instead of stdout because the cljs compiler
    ;; writes stdout to the raw JS file.
    (binding [*out* *err*]
      (println "Building with environment:" env))
    env))


(defmacro read-from-env-path
  [env-name]
  (let [path-from-env (System/getenv env-name)]
    (when-not (nil? path-from-env)
      (clojure.core/slurp path-from-env))))


(defmacro slurp
  [file]
  (clojure.core/slurp file))
