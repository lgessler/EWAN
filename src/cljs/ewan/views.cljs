(ns ewan.views
  (:require [re-frame.core :as rf]
            [re-com.core :as re-com]
            [ewan.events :as events]
            [ewan.project.core :refer [project-select-panel
                                       project-edit-panel]]
            [cljsjs.material-ui]
            [cljs-react-material-ui.core :refer [get-mui-theme color]]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as ic]
            [reagent.core :as r]))

(defn- panels [panel-name]
  (case panel-name
    :project-select-panel [project-select-panel]
    :project-edit-panel [project-edit-panel]
    [:div]))

(defn main-panel []
  (r/with-let [active-panel (rf/subscribe [:ewan.core/active-panel])]
    [ui/mui-theme-provider
     {:mui-theme (get-mui-theme
                  (.-LightRawTheme js/MaterialUIStyles))}
     [:div
      [ui/app-bar {:title "ewan"
                   :show-menu-icon-button false}]
      [panels @active-panel]]]))
