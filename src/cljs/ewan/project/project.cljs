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

;; used in events.cljs
(def ^:export default-db
  {::projects (list {:eaf "Boogie woogie"})
   ::new-project-dialog-open false})

;; ----------------------------------------------------------------------------
;; spec
;; ----------------------------------------------------------------------------
;; spec for the "project", i.e. a row in PouchDB

(spec/def ::eaf eaf30/eaf?)
(spec/def ::project
  (spec/keys :req-un [::eaf]))

;; ----------------------------------------------------------------------------
;; subs
;; ----------------------------------------------------------------------------
(rf/reg-sub
 ::available-projects
 (fn [db _]
   (::projects db)))

(rf/reg-sub
 ::new-project-dialog-open
 (fn [db _]
   (::new-project-dialog-open db)))

;; ----------------------------------------------------------------------------
;; events
;; ----------------------------------------------------------------------------
(rf/reg-event-db
 ::open-new-project-dialog
 (fn [db _]
   (assoc db ::new-project-dialog-open true)))

(rf/reg-event-db
 ::close-new-project-dialog
 (fn [db _]
   (assoc db ::new-project-dialog-open false)))

;; ----------------------------------------------------------------------------
;; views
;; ----------------------------------------------------------------------------

;; dialog

(defn- open-dialog [] (rf/dispatch [::open-new-project-dialog]))
(defn- close-dialog [] (rf/dispatch [::close-new-project-dialog]))


(defn- new-project-dialog-form []
  [:form {:on-submit #(js/console.log "Submit")}
   [:input {:type "text"
            :placeholder "Name"
            :value "atom"
            :on-change #(js/console.log "Changed")}]])

;; dialog's actions prop expects a js array
(def ^{:private true} new-project-dialog-actions
  #js[(r/as-element [ui/flat-button {:label "Create"
                                     :primary true
                                     :on-click close-dialog}])
      (r/as-element [ui/flat-button {:label "Cancel"
                                     :primary false
                                     :on-click close-dialog}])])

(defn- new-project-dialog []
  (let [open (rf/subscribe [::new-project-dialog-open])]
    [ui/dialog {:title "Create a new project"
                :modal false
                :open @open
                :actions (r/as-element new-project-dialog-actions)
                :on-request-close close-dialog}
     [new-project-dialog-form]]))

(defn- new-project-buttons []
  [:ul {:class-name "new-project__buttons"}
   [:li
    [ui/raised-button {:label "New Project"
                       :label-position "after"
                       :primary true
                       :icon (ic/content-add)
                       :on-click open-dialog}]]
   [:li
    [ui/raised-button {:label "Upload ELAN file"
                       :label-position "after"
                       :primary false
                       :icon (ic/file-file-upload)}]]])

;; panel

(defn project-panel []
  (let [projects (rf/subscribe [::available-projects])]
    [ui/paper {:style {:margin "1em" :padding "1em"}}
     [:h2 "Available projects"]
     [ui/list
      (for [project @projects]
        [ui/list-item {:primary-text (.toString (clj->js project))}])]
     [new-project-buttons]
     [new-project-dialog]]))
