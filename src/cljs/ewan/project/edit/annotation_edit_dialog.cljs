(ns ewan.project.edit.annotation-edit-dialog
  (:require [re-frame.core :as rf]
            [ewan.eaf30 :as eaf30]
            [ewan.common :refer [>evt <sub simple-sub]]
            [cljsjs.material-ui]
            [cljs-react-material-ui.core]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as ic]
            [reagent.core :as r]
            [clojure.walk :as w]))



;; -- re-frame declarations ------------------------------------------------------
;; this state needs to be in re-frame so that consuming components can tell the
;; dialog to open itself
(def ^:private default-db {:project/ann-edit-dialog-open false
                           :project/editing-ann-id nil
                           :project/tier-for-new-ann nil})

(simple-sub :project/ann-edit-dialog-open)
(simple-sub :project/editing-ann-id)
(simple-sub :project/tier-for-new-ann)

(rf/reg-event-db
 :project/open-ann-edit-dialog
 (fn [db [_ {:keys [tier-id ann-id]}]]
   (-> db
       (assoc :project/ann-edit-dialog-open true)
       (assoc :project/tier-for-new-ann tier-id)
       (assoc :project/editing-ann-id ann-id))))

(rf/reg-event-db
 :project/close-ann-edit-dialog
 (fn [db _]
   (-> db
       (assoc :project/ann-edit-dialog-open false))))

(rf/reg-event-db
 :project/init-ann-edit-dialog
 (fn [db _]
   (merge db default-db)))

(defn- pouchdb-effect-map
  [new-doc]
  {:method "put"
   :args [new-doc
          (fn [err res]
            (cond err (throw (js/Error. err))
                  (not (.-ok res)) (throw (js/Error. res))
                  (.-rev res) (>evt
                               [:project/update-project-after-edit
                                (assoc new-doc :_rev (.-rev res))])
                  :else (throw (js/Error. "Unknown error while editing/creating annotation"))))]})

(rf/reg-event-fx
 :project/edit-annotation
 (fn [{:keys [db]} [_ v]]
   (let [ann-id (:project/editing-ann-id db)
         eaf (get-in db [:project/current-project :eaf])
         new-db (assoc-in db
                          [:project/current-project :eaf]
                          (eaf30/edit-annotation eaf ann-id v))
         new-doc (:project/current-project new-db)]
     {:db new-db
      :pouchdb (pouchdb-effect-map new-doc)})))

(rf/reg-event-fx
 :project/create-annotation
 (fn [{:keys [db]} [_ v]]
   (let [tier-id (:project/tier-for-new-ann db)
         selection-start (:project/selection-start db)
         selection-end (:project/selection-end db)
         current-time (-> db :project/playback :time)
         eaf (-> db :project/current-project :eaf)
         new-db (assoc-in db
                          [:project/current-project :eaf]
                          (eaf30/insert-annotation
                           eaf
                           {:tier-id tier-id
                            :click-time current-time
                            :start-time selection-start
                            :end-time selection-end
                            :value v}))
         new-doc (:project/current-project new-db)]
     {:db new-db
      :pouchdb (pouchdb-effect-map new-doc)})))

(rf/reg-event-db
 :project/update-project-after-edit
 (fn [db [_ doc]]
   (js/console.log "Searching " (count (:ewan.project.core/projects db)) " docs...")
   (-> db
       (assoc :project/current-project doc)
       (assoc :ewan.project.core/projects
              (map (fn [old]
                     (if (= (:_id doc) (:_id old))
                       doc
                       old))
                   (:ewan.project.core/projects db))))))

(rf/reg-sub
 :project/cv-entries
 :<- [:project/current-eaf]
 (fn [eaf [_ ann-id]]
   (eaf30/get-controlled-vocabulary-entries
    eaf
    (eaf30/get-tier-of-ann eaf ann-id))))

(rf/reg-sub
 :project/ann-value-from-id
 :<- [:project/current-eaf]
 (fn [eaf [_ ann-id]]
   (eaf30/get-annotation-value eaf ann-id)))

(rf/reg-sub
 :project/ann-uses-cv
 :<- [:project/current-eaf]
 :<- [:project/editing-ann-id]
 :<- [:project/selected-tier]
 (fn [[eaf ann-id selected-tier-id] _]
   (if ann-id
     (let [tier-id (eaf30/get-tier-of-ann eaf ann-id)]
       (eaf30/has-controlled-vocabulary eaf tier-id))
     (eaf30/has-controlled-vocabulary eaf selected-tier-id))))

;; -- the rest -----------------------------------------------------------------

(defn- submit
  [state]
  (let [ann-id (:ann-id @state)]
    (rf/dispatch-sync [:project/close-ann-edit-dialog])
    (if ann-id
      (>evt [:project/edit-annotation (:value @state)])
      (>evt [:project/create-annotation (:value @state)]))))

(defn- freetext-value-field
  [state]
  [:div
   [:label {:for "annotation-edit-value-field"} "Annotation value"]
   [ui/text-field
    {:id "annotation-edit-value-field"
     :auto-focus "autofocus"
     :full-width true
     :floating-label-fixed true
     :default-value (:value @state)
     :on-change (fn [_ v] (swap! state assoc :value v))}]])

(defn- cv-value-field
  [state]
  (let [cv-entries (<sub [:project/cv-entries (:ann-id @state)])]
    [ui/select-field
     {:floating-label-text "Annotation value"
      :full-width true
      :floating-label-fixed true
      :value (:value @state)
      :on-change (fn [_ v]
                   (swap! state assoc :value (:value (nth cv-entries v))))}
     (for [{:keys [id value description]} cv-entries]
       [ui/menu-item {:key id
                      :value value
                      :primary-text description}])]))

(defn- form [state ann-id]
  [:form#annotation-edit-form
   {:on-submit (fn [e]
                 (.preventDefault e)
                 (submit state))}
   (if (<sub [:project/ann-uses-cv])
     [cv-value-field state]
     [freetext-value-field state])])

(defn- actions [editing]
  #js[(r/as-element
       [ui/flat-button {:label (if editing "Save" "Create")
                        :primary true
                        :type "submit"
                        :form "annotation-edit-form"}])
      (r/as-element
       [ui/flat-button {:label "Cancel"
                        :primary false
                        :on-click #(>evt [:project/close-ann-edit-dialog])}])])

(defn annotation-edit-dialog
  []
  (r/create-class
   {:component-will-mount #(>evt [:project/init-ann-edit-dialog])
    :reagent-render
    (fn []
      (let [open (or (<sub [:project/ann-edit-dialog-open]) false)
            ann-id (<sub [:project/editing-ann-id])
            tier-id (<sub [:project/tier-for-new-ann])
            default-value (<sub [:project/ann-value-from-id ann-id])
            state (r/atom {:value default-value
                           :ann-id ann-id
                           :tier-id tier-id})]
        [ui/dialog {:title (if ann-id "Edit Annotation" "Create Annotation")
                    :open open
                    :actions (r/as-element (actions (some? ann-id)))
                    :on-request-close #(>evt [:project/close-ann-edit-dialog])}
         [form state ann-id]]))}))
