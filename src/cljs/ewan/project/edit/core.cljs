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
            :on-loaded-metadata #(>evt [:project/record-duration (-> % .-target .-duration)])
            :on-time-update
            #(>evt [:project/time-updated (-> % .-target .-currentTime)])}]
          [:div "Audio"]))})))

(defn- time-container [playback]
  [:div.media-panel__time-container
   (state/time-format (:time @playback))])

(defn- playback-button
  [{:keys [on-click icon-name]}]
  [ui/icon-button {:icon-class-name "material-icons"
                   :icon-style {:width "24px" :height "24px"}
                   :style {:width "36px" :height "36px" :padding "6px"}
                   :on-click on-click}
   icon-name])

(defn- playback-buttons [playback]
  (let [elt (rf/subscribe [:project/media-element])]
    [:div.media-panel__playback-buttons
     [playback-button {:icon-name "first_page"
                       :on-click (fn []
                                   (>evt [:project/stop-playback])
                                   (>evt [:project/set-scroll-left 0])
                                   (state/set-time! @elt 0))}]
     [playback-button {:icon-name "replay_5"
                       :on-click (fn []
                                   (>evt [:project/stop-playback])
                                   (state/add-time! @elt -5))}]
     [playback-button {:icon-name "navigate_before"
                       :on-click (fn []
                                   (>evt [:project/stop-playback])
                                   (state/add-time! @elt -0.02))}]
     [playback-button {:icon-name (if (:play @playback) "pause" "play_arrow")
                       :on-click (fn []
                                   (>evt [:project/toggle-playback]))}]
     [playback-button {:icon-name "navigate_next"
                       :on-click (fn []
                                   (>evt [:project/stop-playback])
                                   (state/add-time! @elt 0.02))}]
     [playback-button {:icon-name "forward_5"
                       :on-click (fn []
                                   (>evt [:project/stop-playback])
                                   (state/add-time! @elt 5))}]
     [playback-button {:icon-name "last_page"
                       :on-click (fn []
                                   (>evt [:project/stop-playback])
                                   (state/set-time! @elt :end))}]]))

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
       [upper-panel]
       [tiers/tiers]]
      [:div.page-loading
       [ui/circular-progress {:size 80
                              :thickness 7}]])))



