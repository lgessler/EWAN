(ns ewan.events
  (:require [re-frame.core :as rf]
            [ewan.db :as db]
            [cljsjs.pouchdb]))

;; ------------------------------------------------------------
;; pdb
;; ------------------------------------------------------------

(def pdb-todos (js/PouchDB. "todos"))
;; allDocs, bulkDocs([docs])

;; ------------------------------------------------------------
;; pdb effects
;; ------------------------------------------------------------

(defn- pouchdb-effect
  [{:as request
    :keys [method args]}]
  (if-not (#{"allDocs" "bulkDocs"} method)
    (throw (js/Error. (str "Unsupported PouchDB method: " method))))


  (let [combined-args (clj->js (into [pdb-todos method] args))]
    (js/console.log "Attempting to invoke:" combined-args)
    (apply js-invoke combined-args)))

(rf/reg-fx :pouchdb pouchdb-effect)

;; ------------------------------------------------------------
;; db events
;; ------------------------------------------------------------

;; TODO: when the docs are loaded, they have pdb's internal structure, but we assumed that
;; the structure we'd need to deal with was the one under the "doc" key of pdb's internal
;; structure. Need to either store everything together (complicating the app data structure)
;; or store pdb's internal information elsewhere.

;; initialization
(rf/reg-event-fx
 ::initialize-db
 (fn [{:keys [db]} _]
   {:db db/default-db
    :pouchdb {:method "allDocs"
              :args [{:include_docs true}
                     (fn [error result]
                       (if error
                         (throw error)
                         (rf/dispatch [::pdb-docs-loaded result])))]}}))

(rf/reg-event-db
 ::pdb-docs-loaded
 (fn [db [_ docs]]
   (js/console.log (js->clj docs))
   (update db :ewan.todos/todos into (js->clj docs))))

(rf/reg-event-fx
 ::save-pdb-docs
 (fn [{:keys [db]} _]
   {:db db
    :pouchdb {:method "bulkDocs"
              :args [(:ewan.todos/todos db)
                     {}
                     (fn [error result]
                       (if error
                         (throw error)
                         (rf/dispatch [::pdb-docs-saved result])))]}}))

(rf/reg-event-db
 ::pdb-docs-saved
 (fn [db [_ responses]]
   (js/console.log "Docs saved! responses:")
   (js/console.log responses)))

;; navigation
(rf/reg-event-db
 ::set-active-panel
 (fn [db [_ active-panel]]
   (assoc db :active-panel active-panel)))

