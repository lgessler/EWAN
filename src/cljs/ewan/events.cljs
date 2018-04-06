(ns ewan.events
  (:require [re-frame.core :as rf]
            [ewan.db :as db]
            [ewan.project.core :as project]
            [cljsjs.pouchdb]))

;; ------------------------------------------------------------
;; pdb
;; ------------------------------------------------------------

;; TODO: allow the selection of a database. For now, just use
;; a single DB
(def current-db (js/PouchDB. "default-db"))

;; ------------------------------------------------------------
;; pdb effects
;; ------------------------------------------------------------

(defn- pouchdb-effect
  [{:as request
    :keys [method args]}]
  (if-not (#{"allDocs" "bulkDocs"} method)
    (throw
     (js/Error. (str "Unsupported PouchDB method: " method))))

  (let [combined-args (clj->js
                       (into [current-db method] args))]
    (js/console.log "Attempting to invoke:" combined-args)
    (apply js-invoke combined-args)))

(rf/reg-fx :pouchdb pouchdb-effect)

;; ------------------------------------------------------------
;; db events
;; ------------------------------------------------------------

;; initialization
(rf/reg-event-fx
 ::initialize-db
 (fn [{:keys [db]} _]
   {:db
    (merge db/default-db
           project/default-db)
    :pouchdb
    {:method "allDocs"
     :args [{:include_docs true}
            (fn [error result]
              (if error
                (throw error)
                (rf/dispatch [::pdb-docs-loaded result])))]}}))

(rf/reg-event-db
 ::pdb-docs-loaded
 (fn [db [_ docs]]
   ;; ignore errors and row count, for now
   (js/console.log docs)
   (let [rows (:rows (js->clj docs :keywordize-keys true))
         docs (map #(:doc %) rows)]
     (js/console.log rows)
     (js/console.log (doall docs))
     (update db ::project/projects into docs))))

(rf/reg-event-fx
 ::save-pdb-docs
 (fn [{:keys [db]} _]
   {:db db
    :pouchdb
    {:method "bulkDocs"
     :args [(clj->js (::project/projects db))
            {}
            (fn [error result]
              (if error
                (throw error)
                (rf/dispatch [::pdb-docs-saved result])))]}}))

(rf/reg-event-db
 ::pdb-docs-saved
 (fn [db [_ responses]]
   (js/console.log "Docs saved! responses:")
   (js/console.log responses)
   ;; ignore errors for now

   (assoc db
          ::project/projects
          (map (fn [row response]
                 (-> row
                     (assoc :_id (get response "id"))
                     (assoc :_rev (get response "rev"))))
               (::project/projects db)
               (js->clj responses)))))

;; navigation
(rf/reg-event-db
 ::set-active-panel
 (fn [db [_ active-panel]]
   (assoc db :active-panel active-panel)))

