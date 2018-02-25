(ns ewan.db
  (:require [re-frame.core :as re-frame]
            [ewan.todos]))

(def default-db
  {:ewan.todos/todos #{} 
   :ewan.todos/current-todo ""})
