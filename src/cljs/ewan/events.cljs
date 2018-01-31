(ns ewan.events
  (:require [re-frame.core :as re-frame]
            [ewan.db :as db]
            [cljsjs.pouchdb :as pdb]))

;; interceptors

(def tuck-in-pouch
  (re-frame/->interceptor
   :id :tuck-in-pouch
   :after (fn [context]
            "I don't do anything... yet!"
            context)))

;; db events

(re-frame/reg-event-db
 ::initialize-db
 (fn  [_ _]
   db/default-db))

(re-frame/reg-event-db
 ::set-active-panel
 (fn [db [_ active-panel]]
   (assoc db :active-panel active-panel)))

(re-frame/reg-event-db
 ::update-current-todo
 (fn [db [_ text]]
   (assoc db :current-todo text)))

(re-frame/reg-event-db
 ::add-current-todo
 (fn [db [_ new-text]]
   (-> db
       (update :todos conj (:current-todo db))
       (assoc :current-todo ""))))

