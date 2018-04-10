(ns ewan.project.core
  (:require [re-frame.core :as rf]
            [ewan.spec.eaf30 :as eaf30]
            [ewan.project.form :refer [new-project-dialog-form]]
            [cljsjs.material-ui]
            [cljs-react-material-ui.core]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as ic]
            [reagent.core :as r]
            [cljs.spec.alpha :as spec])
  (:require-macros [cljs.spec.alpha :as spec]))

;; used in events.cljs
(def ^:export default-db
  {::projects []
   ::new-project-dialog-open false})


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

;; When a user submits the create new project form, this event is fired.
(rf/reg-event-fx
 ::create-new-project
 (fn [{:keys [db]} [_ {:keys [:name :author :date :files] :as state}]]
   (let [doc {:name name
              :eaf (eaf30/create-eaf {:author author
                                      :date (.toISOString date)
                                      :media-descriptors (for [file files]
                                                           {:media-url (.-name file)
                                                            :mime-type (.-type file)})})
              :_attachments
              (into {} (for [file files]
                         [(.-name file) {:content_type (.-type file)
                                         :data file}]))}]
     {:db db
      :pouchdb
      {:method "post"
       :args [doc
              {}
              (fn [err response]
                (if err
                  (throw err)
                  (rf/dispatch [::new-project-created response])))]}})))

;; Because PouchDB's `post` method is async, ::create-new-project initiates
;; the creation of the PDB document, and this event is fired after the
;; document is created.
;; TODO: proper error handling
(rf/reg-event-fx
 ::new-project-created
 (fn [{:keys [db]} [_ response]]
   (if-not (.-ok response)
     (throw response)
     {:db db
      :pouchdb {:method "get"
                :args [(.-id response)
                       {}
                       (fn [err doc]
                         (if err
                           (throw err)
                           (rf/dispatch [::new-project-doc-fetched doc])))]}})))

;; Since `post` only tells you whether or not the document was successfully
;; created and does not give you the document's value, we need to call `get`
;; so we can update re-frame's DB with the value of the new document.
(rf/reg-event-db
 ::new-project-doc-fetched
 (fn [db [_ doc]]
   (update db ::projects conj (js->clj doc :keywordize-keys true))))

;; ----------------------------------------------------------------------------
;; views
;; ----------------------------------------------------------------------------

;; dialog -- a popup window that contains a form for creating a new project

(defn- open-dialog [] (rf/dispatch [::open-new-project-dialog]))
(defn- close-dialog [] (rf/dispatch [::close-new-project-dialog]))

;; dialog's actions prop expects a js array
(def ^{:private true} new-project-dialog-actions
  #js[(r/as-element [ui/flat-button {:label "Create"
                                     :primary true
                                     :type "submit"
                                     :form "new-project-dialog-form"}])
      (r/as-element [ui/flat-button {:label "Cancel"
                                     :primary false
                                     :on-click close-dialog}])])

(defn- form-submitted-callback
  [state]
  (close-dialog)
  (rf/dispatch [::create-new-project state]))

(defn- new-project-dialog []
  (let [open (rf/subscribe [::new-project-dialog-open])] ;; for form ID
    [ui/dialog {:title "Create a new project"
                :modal false
                :open @open
                :actions (r/as-element new-project-dialog-actions)
                :on-request-close close-dialog
                :auto-scroll-body-content true}
     [new-project-dialog-form form-submitted-callback]]))

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

;; panel -- top level element

(defn project-panel []
  (let [projects (rf/subscribe [::available-projects])]
    [ui/paper {:style {:margin "1em" :padding "1em"}}
     [:h2 "Available projects"]
     [ui/list
      (for [project @projects]
        [ui/list-item {:primary-text (:name project)
                       :secondary-text (-> project
                                           :eaf
                                           eaf30/get-date
                                           js/Date.
                                           .toLocaleDateString)
                       :key (:_id project)}])]
     [new-project-buttons]
     [new-project-dialog]]))
