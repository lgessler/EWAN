(ns ewan.eaf30.macros)

(defmacro defzipfn
  [fname & xs]
  `(defn ~fname
     [hiccup#]
     (-> hiccup# ewan.eaf30/hiccup-zipper ~@xs)))

(defmacro defzipfn-
  [fname & xs]
  `(defn- ~fname
     [hiccup#]
     (-> hiccup# ewan.eaf30/hiccup-zipper ~@xs)))
