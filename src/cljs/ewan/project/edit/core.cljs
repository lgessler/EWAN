(ns ewan.project.edit.core
  (:require [re-frame.core :as rf]
            [ewan.common :refer [<sub >evt]]
            [ewan.spec.eaf30 :as eaf30]
            [ewan.project.edit.tiers :as tiers]
            [ewan.project.edit.state :as state]
            [cljsjs.material-ui]
            [cljs-react-material-ui.core]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as ic]
            [reagent.core :as r]))

(defn- media-panel-inner [media-map]
  (let [update
        (fn [comp]
          (let [{:keys [play src media-element] :as playback} (r/props comp)
                newsrc (:src (r/props comp))]

            ;; TODO: it's really NOT a good idea to have the element in the
            ;; DB, since it's not serializable and in principle, since
            ;; the element itself is mutable (even though its reference isn't)
            ;; the element could disappear from the DB. Cleaner solution
            ;; would involve creating an atom for the ref, but I couldn't get
            ;; that to work very well when I tried. Global `def` is probably
            ;; even worse. This'll do for now.
            (when (some? media-element)
              (when (not= newsrc (.-src media-element))
                (set! (.-src media-element) newsrc))
              (cond (and play (.-paused media-element)) (.play media-element)
                    (not play) (.pause media-element)))))]
    (r/create-class
     {:component-did-update update
      :component-did-mount (fn [comp]
                             (update comp))
      :reagent-render
      (fn [media-map]
        (if (= (:type media-map) :video)
          [:video.media-panel__video
           {:ref #(>evt [:project/set-media-element %])
            :on-loaded-metadata
            #(>evt [:project/record-duration (-> % .-target .-duration)])
            :on-time-update
            #(>evt [:project/time-updated (-> % .-target .-currentTime)])}]
          [:div "Audio"]))})))

(defn- time-container [playback]
  [:div.media-panel__time-container
   (state/time-format (:time @playback))])

;; playback buttons
(def ^:private button-style {:width "36px" :height "36px"
                             :padding "6px" :display "flex"})

(defn- playback-button
  [{:keys [on-click icon style]}]
  [ui/icon-button
   {:style (merge button-style style)
    :on-click on-click
    :icon-class-name "material-icons"
    :icon-style {:width "24px" :height "24px"}}
   icon])

(defn- play-selection-svg []
  [:svg {:width "24"
         :height "24"
         :view-box "0 0 24 24"
         :xmlns "http://www.w3.org/2000/svg"}
   [:text {:x 12 :y 16
           :style {:font-size "12px"
                   :font-weight "500"
                   :user-select "none"}}
    "S"]
   [:path {:d "M4 17l5-5-5-5v10z"}]
   [:path {:fill "none" :d "M0 24V0h24v24H0z"}]])

(defn- play-selection-button []
  (let [start (<sub [:project/selection-start])
        elt (<sub [:project/media-element])]
    [ui/icon-button
     {:style button-style
      :disabled (not (<sub [:project/playable-selection?]))
      :on-click (fn [e]
                  ;; NYI: pause at the end of the selection
                  (state/set-time! elt start)
                  (rf/dispatch-sync [:project/start-playback]))}
     (play-selection-svg)]))

(defn- clear-selection-svg []
  [:svg {:width "24"
         :height "24"
         :view-box "0 0 24 24"
         :xmlns "http://www.w3.org/2000/svg"}
   [:text {:x 8 :y 16
           :style {:font-size "14px"
                   :font-weight "400"
                   :user-select "none"}}
    "S"]
   [:line {:x1 14 :y1 4 :x2 10 :y2 18 :style {:stroke "red" :stroke-width 1}}]
   [:path {:fill "none" :d "M0 24V0h24v24H0z"}]])

(defn- clear-selection-button []
  [ui/icon-button
   {:style button-style
    :on-click #(>evt [:project/clear-selection])}
   (clear-selection-svg)])

(defn- playback-buttons [playback]
  (let [elt (<sub [:project/media-element])]
    [:div.media-panel__playback-buttons
     [playback-button {:icon "first_page"
                       :on-click (fn []
                                   (rf/dispatch-sync [:project/stop-playback])
                                   (>evt [:project/set-scroll-left 0])
                                   (state/set-time! elt 0))}]
     [playback-button {:icon "replay_5"
                       :on-click (fn []
                                   (rf/dispatch-sync [:project/stop-playback])
                                   (state/add-time! elt -5))}]
     [playback-button {:icon "navigate_before"
                       :on-click (fn []
                                   (rf/dispatch-sync [:project/stop-playback])
                                   (state/add-time! elt -0.02))}]
     [playback-button {:icon (if (:play @playback) "pause" "play_arrow")
                       :on-click (fn []
                                   (rf/dispatch-sync [:project/toggle-playback]))}]
     [playback-button {:icon "navigate_next"
                       :on-click (fn []
                                   (rf/dispatch-sync [:project/stop-playback])
                                   (state/add-time! elt 0.02))}]
     [playback-button {:icon "forward_5"
                       :on-click (fn []
                                   (rf/dispatch-sync [:project/stop-playback])
                                   (state/add-time! elt 5))}]
     [playback-button {:icon "last_page"
                       :on-click (fn []
                                   (rf/dispatch-sync [:project/stop-playback])
                                   (state/set-time! elt :end))}]
     [playback-button {:icon "blank" :style {:visibility "hidden"}}]
     [play-selection-button]
     [clear-selection-button]]))

(defn- media-panel-outer []
  (let [playback (rf/subscribe [:project/playback])]
    (fn []
      [ui/paper {:style {:width "50%"
                         :max-width "480px"
                         :display "flex"
                         :flex-direction "column"
                         :margin "6px"
                         :padding "8px"}}
       [media-panel-inner @playback]
       [time-container playback]
       [playback-buttons playback]])))

;; upper right panel ----------------------------------------------------------
(defn- upper-right-panel []
  [ui/paper {:style {:width "100%"
                     :margin "6px"
                     :padding "8px"}}])

(defn- upper-panel []
  [:div.upper-panel
   [media-panel-outer]
   [upper-right-panel]])

;; root element ---------------------------------------------------------------
(defn project-edit-panel-body []
  (r/with-let [doc (rf/subscribe [:project/current-project])
               playback (rf/subscribe [:project/playback])
               loaded (rf/subscribe [:project/loaded])]
    (if @loaded
      [:div
       #_{:on-key-down ;; doesn't work
          (fn [e]
            (js/console.log "detected keypress")
            (if (and (.ctrlKey e)
                     (= (.key e) "b"))
              (js/console.log "CTRL+b detected")))}
       {:style {:visibility (if @loaded "unset" "hidden")}}
       [:div.page-loading
        {:style {:display (if @loaded "none" "unset")}}
        [ui/circular-progress {:size 80
                               :thickness 7}]]
       [upper-panel]
       [tiers/tiers]])))



