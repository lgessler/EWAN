(ns ewan.eaf30)

(defmacro defzipfn
  [fname & xs]
  `(defn ~fname
     [hiccup#]
     (-> hiccup# hiccup-zipper ~@xs)))

(defmacro defzipfn-
  [fname & xs]
  `(defn- ~fname
     [hiccup#]
     (-> hiccup# hiccup-zipper ~@xs)))
