(ns ewan.core
  (:require [re-frame.core :as rf]
            [reagent.core :as reagent]
            [ewan.db :as db]
            [ewan.routes :as routes]
            [ewan.views :as views]))

;; debug setup
(def debug?
  ^boolean goog.DEBUG)

(defn dev-setup []
  (when debug?
    (enable-console-print!)
    (println "dev mode")))

;; setup
(defn mount-root []
  (rf/clear-subscription-cache!)
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (routes/app-routes)
  (rf/dispatch-sync [::db/initialize-db])
  (dev-setup)
  (mount-root))
