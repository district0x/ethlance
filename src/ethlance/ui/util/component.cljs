(ns ethlance.ui.util.component
  "Includes utilities for working with re-frame components")


(defn unwrap-seq
  "Unwraps a sequence argument if it contains one element.

  # Examples

  (defn foo [& x]
    (unwrap-seq x))

  ;; (foo)     => ()
  ;; (foo 1)   => 1
  ;; (foo 1 2) => (2 3)"
  [x]
  (if (and (seq? x) (= (count x) 1))
    (first x)
    x))
