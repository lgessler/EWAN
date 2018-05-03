(ns ewan.common
  (:require [re-frame.core :as rf]))

;; hiccup helpers
(defn tag-name [hiccup] (first hiccup))
(defn attrs [hiccup] (second hiccup))
(defn first-child [hiccup] (nth hiccup 2))
(defn children [hiccup] (drop 2 hiccup))

;; re-frame shorthands
(def <sub (comp deref rf/subscribe))
(def >evt rf/dispatch)

(defn- simple-sub
  "Convenience function for subs that just take a single value out of `db`"
  ([kwd]
   (rf/reg-sub kwd (fn [db _] (kwd db))))
  ([name path]
   (rf/reg-sub name (fn [db _] (get-in db path)))))



