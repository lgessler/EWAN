(ns ewan.todos
  (:require [re-frame.core :as rf]
            [re-com.core :as re-com]
            [ewan.events]))

; :ewan.todos/todos #{} 
; :ewan.todos/current-todo ""

;; subs

(rf/reg-sub
 ::todos
 (fn [db _]
   (::todos db)))

(rf/reg-sub
 ::current-todo
 (fn [db _]
   (::current-todo db)))

;; events

(rf/reg-event-db
 ::update-current-todo
 (fn [db [_ text]]
   (assoc db ::current-todo text)))

(rf/reg-event-fx
 ::add-current-todo
 (fn [{:keys [db]} [_ new-text]]
   {:db (-> db
          (update ::todos conj {"task" (::current-todo db)})
          (assoc ::current-todo ""))
    :dispatch [:ewan.events/save-pdb-docs]}))

;; views

(defn enter-todo []
  (let [current-todo (rf/subscribe [::current-todo])]
    (fn []
      [re-com/input-text
       :model current-todo
       :on-change #(rf/dispatch [::update-current-todo %])
       :change-on-blur? false
       :placeholder "Bold and brash"]
      )))

(defn ^:export todo-list []
  (let [todos (rf/subscribe [::todos])]
    (fn []
      [:ul
       (for [{:strs [task]} @todos]
         [:li {:key task}
          [:p task]])])))

(defn ^:export todo-form []
  [:form {:on-submit
          (fn [e]
            (.preventDefault e)
            (when (> (count @(rf/subscribe [::current-todo])) 0)
              (rf/dispatch [::add-current-todo])))}
   [enter-todo]])

(def ^:export default-db
  {::todos #{}
   ::current-todo ""})
