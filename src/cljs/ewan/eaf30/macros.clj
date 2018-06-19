(ns ewan.eaf30.macros)

(defmacro defzipfn
  [fname & xs]
  `(defn ~fname
     [hiccup#]
     (-> hiccup# ewan.eaf30.core/hiccup-zipper ~@xs)))

(defmacro defzipfn-
  [fname & xs]
  `(defn- ~fname
     [hiccup#]
     (-> hiccup# ewan.eaf30.core/hiccup-zipper ~@xs)))
