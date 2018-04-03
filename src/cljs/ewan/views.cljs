(ns ewan.views
  (:require [re-frame.core :as re-frame]
            [re-com.core :as re-com]
            [ewan.subs :as subs]
            [ewan.events :as events]
            [ewan.project :refer [project-panel]]
            [cljsjs.material-ui]
            [cljs-react-material-ui.core :refer [get-mui-theme color]]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as ic]
            [reagent.core :as r]))

(defn- panels [panel-name]
  (case panel-name
    :project-panel [project-panel]
    [:div]))

(defn main-panel []
  (let [active-panel (re-frame/subscribe [::subs/active-panel])]
    [ui/mui-theme-provider
     {:mui-theme (get-mui-theme
                  (.-LightRawTheme js/MaterialUIStyles))}
     [:div
      [ui/app-bar {:title "ewan"
                   :icon-element-right
                   (r/as-element [ui/icon-button
                                  (ic/action-feedback)])}]
      [panels @active-panel]]]))
