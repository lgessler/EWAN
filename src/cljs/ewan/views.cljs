(ns ewan.views
  (:require [re-frame.core :as re-frame]
            [re-com.core :as re-com]
            [ewan.subs :as subs]
            [ewan.events :as events]
            ))

;; home

(defn link-to-about-page []
  [re-com/hyperlink-href
   :label "go to About Page"
   :href "#/about"])

(defn enter-todo []
  (let [current-todo (re-frame/subscribe [::subs/current-todo])]
    (fn []
      [re-com/input-text
       :model current-todo
       :on-change #(re-frame/dispatch [::events/update-current-todo %])
       :change-on-blur? false
       :placeholder "Bold and brash"]
      )))

(defn todo-list []
  (let [todos (re-frame/subscribe [::subs/todos])]
    (fn []
      [:ul
       (for [todo @todos]
         [:li {:key todo}
          [:p todo]])])))

(defn todo-form []
  [:form {:on-submit (fn [e]
                       (.preventDefault e)
                       (re-frame/dispatch [::events/add-current-todo]))}
   [enter-todo]])

(defn home-panel []
  [re-com/v-box
   :gap "1em"
   :children [[todo-list]
              [todo-form]
              [link-to-about-page]]])


;; about

(defn about-title []
  [re-com/title
   :label "This is the About Page."
   :level :level1])

(defn link-to-home-page []
  [re-com/hyperlink-href
   :label "go to Home Page"
   :href "#/"])

(defn about-panel []
  [re-com/v-box
   :gap "1em"
   :children [[about-title] [link-to-home-page]]])


;; main

(defn- panels [panel-name]
  (case panel-name
    :home-panel [home-panel]
    :about-panel [about-panel]
    [:div]))

(defn show-panel [panel-name]
  [panels panel-name])

(defn main-panel []
  (let [active-panel (re-frame/subscribe [::subs/active-panel])]
    [re-com/v-box
     :height "100%"
     :children [[panels @active-panel]]]))
