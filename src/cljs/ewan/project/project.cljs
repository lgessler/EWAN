(ns ewan.project
  (:require [re-frame.core :as rf]
            [re-com.core :as re-com]
            [ewan.eaf30 :as eaf30]
            [cljsjs.material-ui]
            [cljs-react-material-ui.core]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as ic]
            [reagent.core :as r]
            [cljs.spec.alpha :as spec])
  (:require-macros [cljs.spec.alpha :as spec]))

;; spec for the "project", i.e. a row in PouchDB

(spec/def ::eaf eaf30/eaf?)
(spec/def ::project
  (spec/keys :req-un [::eaf]))

;; used in events.cljs
(def ^:export default-db
   {::projects (list {:eaf "Boogie woogie"})
    ::add-project-modal-deployed false})

;; subs
(rf/reg-sub
 ::available-projects
 (fn [db _]
   (::projects db)))

;; events


;; views

(defn project-panel []
  (let [projects (rf/subscribe [::available-projects])]
    [:div {:style {:margin "1em"}}
     "Available projects"
     [ui/list
      (for [project @projects]
        [ui/list-item {:primary-text (.toString (clj->js project))}])]
     [ui/raised-button {:label "+"}]
     ]))
