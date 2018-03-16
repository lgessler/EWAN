(ns ewan.views
  (:require [re-frame.core :as re-frame]
            [re-com.core :as re-com]
            [ewan.subs :as subs]
            [ewan.events :as events]
            [ewan.todos :as todos]
            [cljsjs.material-ui]
            [cljs-react-material-ui.core :refer [get-mui-theme color]]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as ic]
            [reagent.core :as r]
            ))

;; home

(defn link-to-about-page []
  [re-com/hyperlink-href
   :label "go to About Page"
   :href "#/about"])

(defn home-panel []
  [re-com/v-box
   :gap "1em"
   :children [[todos/todo-list]
              [todos/todo-form]
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
    [ui/mui-theme-provider
     {:mui-theme (get-mui-theme
                  {:palette {:text-color (color :green600)}})}
     [:div
      [ui/app-bar {:title "Title"
                   :icon-element-right
                   (r/as-element [ui/icon-button
                                  (ic/action-account-balance-wallet)])}]
      [ui/paper
       [:div "Hello"]
       [ui/mui-theme-provider
        {:mui-theme (get-mui-theme {:palette {:text-color (color :blue200)}})}
        [ui/raised-button {:label "Blue button"}]]
       (ic/action-home {:color (color :grey600)})
       [ui/raised-button {:label        "Click me"
                          :icon         (ic/social-group)
                          :on-click     #(println "clicked")}]]
      [panels @active-panel]]]))
