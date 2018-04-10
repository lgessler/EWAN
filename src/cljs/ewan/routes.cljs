(ns ewan.routes
  (:require-macros [secretary.core :refer [defroute]])
  (:import goog.History)
  (:require [secretary.core :as secretary]
            [goog.events :as gevents]
            [goog.history.EventType :as EventType]
            [re-frame.core :as re-frame]
            [ewan.events :as events]
            [ewan.project.core :as project]
            [ewan.project.edit :as prjedit]
            ))

(defn hook-browser-navigation! []
  (doto (History.)
    (gevents/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

(defn app-routes []
  (secretary/set-config! :prefix "#")
  ;; --------------------
  ;; define routes here
  (defroute "/" []
    ;; For now, just go to project selection
    ;; This sets the window's location hash, but unnecessary
    ;(aset (.-location js/window) "hash" "#/project")
    (secretary/dispatch! "/project"))

  (defroute "/project" []
    (re-frame/dispatch [::events/set-active-panel :project-select-panel]))

  (defroute "/project/:id" [id]
    (re-frame/dispatch [::prjedit/open-project id]))


  ;; --------------------
  (hook-browser-navigation!))
