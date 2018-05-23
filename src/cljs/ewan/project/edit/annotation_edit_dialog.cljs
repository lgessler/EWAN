(ns ewan.project.edit.annotation-edit-dialog
  (:require [re-frame.core :as rf]
            [ewan.spec.eaf30 :as eaf30]
            [ewan.common :refer [>evt <sub simple-sub]]
            [cljsjs.material-ui]
            [cljs-react-material-ui.core]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as ic]
            [reagent.core :as r]))



;; -- re-frame declarations ------------------------------------------------------
;; this state needs to be in re-frame so that consuming components can tell the
;; dialog to open itself
(def ^:private default-db {:project/ann-edit-dialog-open false
                           :project/editing-ann-id nil})

(simple-sub :project/ann-edit-dialog-open)
(simple-sub :project/editing-ann-id)

(rf/reg-event-db
 :project/open-ann-edit-dialog
 (fn [db [_ ann-id]]
   (-> db
       (assoc :project/ann-edit-dialog-open true)
       (assoc :project/editing-ann-id ann-id))))

(rf/reg-event-db
 :project/close-ann-edit-dialog
 (fn [db _]
   (-> db
       (assoc :project/ann-edit-dialog-open false)
       (assoc :project/editing-ann-id nil))))

(rf/reg-event-db
 :project/init-ann-edit-dialog
 (fn [db _]
   (merge db default-db)))

;; -- the rest -----------------------------------------------------------------

(defn annotation-edit-dialog
  []
  (r/create-class
   {:component-will-mount #(>evt [:project/init-ann-edit-dialog])
    :reagent-render
    (fn []
      (let [open (or (<sub [:project/ann-edit-dialog-open]) false)
            ann-id (<sub [:project/editing-ann-id])]
        [ui/dialog {:title (if ann-id
                             "Edit annotation"
                             "Create annotation")
                    :open open
                    :on-request-close #(>evt [:project/close-ann-edit-dialog])}]))}))
