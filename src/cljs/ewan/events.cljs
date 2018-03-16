(ns ewan.events
  (:require [re-frame.core :as rf]
            [ewan.db :as db]
            [ewan.todos :as todos]
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

;; initialization
(rf/reg-event-fx
 ::initialize-db
 (fn [{:keys [db]} _]
   {:db (merge db/default-db
               todos/default-db)
    :pouchdb {:method "allDocs"
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
   (let [rows (get (js->clj docs) "rows")
         docs (map #(get % "doc") rows)]
     (update db :ewan.todos/todos into docs))))

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
   (js/console.log responses)
   ;; ignore errors for now

   (assoc db
          :ewan.todos/todos
          (map (fn [row response]
                 (-> row
                     (assoc "_id" (get response "id"))
                     (assoc "_rev" (get response "rev"))))
               (:ewan.todos/todos db)
               (js->clj responses)))))

;; navigation
(rf/reg-event-db
 ::set-active-panel
 (fn [db [_ active-panel]]
   (assoc db :active-panel active-panel)))

