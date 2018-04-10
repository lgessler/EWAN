;*CLJSBUILD-MACRO-FILE*;
(ns ewan.spec.eaf30)

(defmacro defzipfn
  [fname & xs]
  `(defn ~fname
     [hiccup#]
     (-> hiccup# hiccup-zipper ~@xs)))
