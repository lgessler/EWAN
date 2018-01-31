(ns ewan.subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::todos
 (fn [db]
   (:todos db)))

(re-frame/reg-sub
 ::active-panel
 (fn [db _]
   (:active-panel db)))

(re-frame/reg-sub
 ::todos
 (fn [db _]
   (:todos db)))

(re-frame/reg-sub
 ::current-todo
 (fn [db _]
   (:current-todo db)))
