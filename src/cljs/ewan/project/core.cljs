(ns ewan.project.core
  (:require [re-frame.core :as rf]
            [ewan.spec.eaf30 :as eaf30]
            [ewan.project.new-form :refer [new-project-dialog-form]]
            [ewan.project.upload-form :refer [upload-project-dialog-form]]
            [ewan.project.edit.core :refer [project-edit-panel-body]]
            [cljsjs.material-ui]
            [cljs-react-material-ui.core]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as ic]
            [reagent.core :as r]))

;; used in db.cljs
(def ^:export default-db
  {::projects []
   ::new-project-dialog-open false
   ::upload-project-dialog-open false})

;; ----------------------------------------------------------------------------
;; subs
;; ----------------------------------------------------------------------------
(rf/reg-sub ::available-projects (fn [db _] (::projects db)))
(rf/reg-sub ::new-project-dialog-open (fn [db _] (::new-project-dialog-open db)))
(rf/reg-sub ::upload-project-dialog-open (fn [db _] (::upload-project-dialog-open db)))

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
(rf/reg-event-db
 ::open-upload-project-dialog
 (fn [db _]
   (assoc db ::upload-project-dialog-open true)))
(rf/reg-event-db
 ::close-upload-project-dialog
 (fn [db _]
   (assoc db ::upload-project-dialog-open false)))

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

;; Similar event, but for uploading a project
(rf/reg-event-fx
 ::upload-new-project
 (fn [{:keys [db]} [_ {:keys [:name :eaf :files] :as state}]]

   (let [doc {:name name
              :eaf eaf
              :_attachments (into {} (for [file files]
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

;; new project dialog ---------------------------------------------------------
(defn- open-new-project-dialog [] (rf/dispatch [::open-new-project-dialog]))
(defn- close-new-project-dialog [] (rf/dispatch [::close-new-project-dialog]))

;; dialog's actions prop expects a js array
(def ^{:private true} new-project-dialog-actions
  #js[(r/as-element [ui/flat-button {:label "Create"
                                     :primary true
                                     :type "submit"
                                     :form "new-project-dialog-form"}])
      (r/as-element [ui/flat-button {:label "Cancel"
                                     :primary false
                                     :on-click close-new-project-dialog}])])

(defn- new-form-submitted-callback
  [state]
  (close-new-project-dialog)
  (rf/dispatch [::create-new-project state]))

(defn- new-project-dialog []
  (r/with-let [open (rf/subscribe [::new-project-dialog-open])] ;; for form ID
    [ui/dialog {:title "Create a new project"
                :modal false
                :open @open
                :actions (r/as-element new-project-dialog-actions)
                :on-request-close close-new-project-dialog
                :auto-scroll-body-content true}
     [new-project-dialog-form new-form-submitted-callback]]))

;; upload project dialog ------------------------------------------------------
(defn- open-upload-project-dialog [] (rf/dispatch [::open-upload-project-dialog]))
(defn- close-upload-project-dialog [] (rf/dispatch [::close-upload-project-dialog]))

;; dialog's actions prop expects a js array
(def ^{:private true} upload-project-dialog-actions
  #js[(r/as-element [ui/flat-button {:label "Create"
                                     :primary true
                                     :type "submit"
                                     :form "upload-project-dialog-form"}])
      (r/as-element [ui/flat-button {:label "Cancel"
                                     :primary false
                                     :on-click close-upload-project-dialog}])])

(defn- upload-form-submitted-callback
  [state]
  (close-upload-project-dialog)
  (rf/dispatch [::upload-new-project state]))

(defn- upload-project-dialog []
  (r/with-let [open (rf/subscribe [::upload-project-dialog-open])] ;; for form ID
    [ui/dialog {:title "Create a project"
                :modal false
                :open @open
                :actions (r/as-element upload-project-dialog-actions)
                :on-request-close close-upload-project-dialog
                :auto-scroll-body-content true}
     [upload-project-dialog-form upload-form-submitted-callback]]))

(defn- new-project-buttons []
  [:ul {:class-name "new-project__buttons"}
   [:li
    [ui/raised-button {:label "New Project"
                       :label-position "after"
                       :primary true
                       :icon (ic/content-add)
                       :on-click open-new-project-dialog}]]
   [:li
    [ui/raised-button {:label "Upload ELAN file"
                       :label-position "after"
                       :primary false
                       :icon (ic/file-file-upload)
                       :on-click open-upload-project-dialog}]]])


;; top level panels
;; -----------------------------------------------------------------------------
;; panel select panel
(defn project-select-panel []
  (r/with-let [projects (rf/subscribe [::available-projects])]
    [ui/paper {:style {:margin "1em" :padding "1em"}}
     [:h2 "Available projects"]
     [ui/list
      (for [project @projects]
        [:a.nostyle {:href (str "#/project/" (:_id project))
                     :key (:_id project)}
         [ui/list-item {:primary-text (:name project)
                        :secondary-text (-> project
                                            :eaf
                                            eaf30/get-date
                                            js/Date.
                                            .toLocaleDateString)}]])]
     [new-project-buttons]
     [new-project-dialog]
     [upload-project-dialog]]))

;; project edit panel
;; -----------------------------------------------------------------------------
(defn project-edit-panel []
  [project-edit-panel-body])
