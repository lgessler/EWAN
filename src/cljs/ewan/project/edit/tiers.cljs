(ns ewan.project.edit.tiers
  (:require [re-frame.core :as rf]
            [cljsjs.material-ui]
            [cljs-react-material-ui.core]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as ic]
            [ewan.common :refer [tag-name attrs first-child children
                                 <sub >evt]]
            [ewan.project.edit.state :as state]
            [reagent.core :as r]
            [goog.functions]))

(defn- alignable-annotation
  [a-ann]
  ;; TODO: find out why time-slot-ref1 and ref2 are sometimes allowed
  ;; to refer to time-slots without a value
  (let [elt (<sub [:project/media-element])
        t (<sub [:project/ann-begin-time a-ann])]
    [:svg (merge (<sub [:project/ann-svg-attrs a-ann])
                 {:style {:pointer-events "bounding-box"}
                  :on-click (fn [e]
                              (.stopPropagation e)
                              (rf/dispatch [:project/stop-playback])
                              (state/set-time! elt t))})
     [:path (<sub [:project/ann-path-attrs a-ann])]
     [:text (<sub [:project/ann-text-attrs])
      (<sub [:project/ann-text-value a-ann])]]))

(def ^:private ref-annotation alignable-annotation)

(defn- annotation
  [[_ _ [ann-type _ _ :as inner-ann]]]
  (condp = ann-type
    :alignable-annotation [alignable-annotation inner-ann]
    :ref-annotation [ref-annotation inner-ann]
    nil))

(defn- tier-row
  [[_ _ & annotations]]
  [:div.tier-rows__row
   [:svg {:width (<sub [:project/tier-width])
          :height (<sub [:project/tier-height])}
    (doall
     (for [ann annotations]
       ^{:key (-> ann first-child attrs :annotation-id)}
       [annotation ann]))]])

(defn- tier-rows [tiers]
  [:div.tier-rows__container
   (doall
    (for [tier @tiers]
      ^{:key (-> tier attrs :tier-id)}
      [tier-row tier]))])

(defn- tier-labels [tiers]
  [:div.tier-labels__container
   {:on-click #(.stopPropagation %)}
   (doall
    (for [tier @tiers]
      (let [tier-id (-> tier attrs :tier-id)]
        ^{:key tier-id}
        [:div.tier-labels__row tier-id])))])

(defn- crosshair []
  (let [{:keys [left]} (<sub [:project/crosshair-display-info])]
    [:div.crosshair {:style {:left (str (+ 100 left) "px")}}]))

(defn- ticks []
  (let [pps (rf/subscribe [:project/px-per-sec])
        duration (rf/subscribe [:project/duration])]
    [:div.ticks__container
     [:div.ticks__spacer
      [ui/icon-button {:icon-class-name "material-icons"
                       :icon-style {:width "18px" :height "18px" :color "inherit"}
                       :style {:width "24px" :height "24px" :padding "3px" :color "#bbb"}
                       :hovered-style {:color "black"}
                       :on-click (fn [e]
                                   (.stopPropagation e)
                                   (>evt [:project/decr-px-per-sec]))}
       "zoom_out"]
      [ui/icon-button {:icon-class-name "material-icons"
                       :icon-style {:width "18px" :height "18px" :color "inherit"}
                       :style {:width "24px" :height "24px" :padding "3px" :color "#bbb"}
                       :hovered-style {:color "black"}
                       :on-click (fn [e]
                                   (.stopPropagation e)
                                   (>evt [:project/incr-px-per-sec]))}
       "zoom_in"]]
     [:svg.ticks {:width (* @pps @duration)}
      ;; use decisecs to avoid float precision issues
      (doall
       (for [decisec (range 0 (+ 10 (* @duration 10)))]
         (let [sec (/ decisec 10)
               x (* sec @pps)]
           (if (= (mod decisec 10) 0)
             [:g {:key decisec}
              [:line {:x1 x :x2 x :y1 0 :y2 6 :stroke-width 0.5 :stroke "black"}]
              [:text {:x x :y 16 :font-size 10 :text-anchor "middle" :style {:user-select "none"}}
               (state/time-format sec)]]
             [:line {:key decisec :x1 x :x2 x :y1 0 :y2 3 :stroke-width 0.5 :stroke "black"}]))))]]))

(defn- tiers-inner
  []
  (r/with-let [tiers (rf/subscribe [:project/tiers])
               !div (atom nil)
               ;; create just one instance of the function that will inform the DB of scroll position
               ;; and debounce it so it will not fire too often (scroll events fire very quickly)
               handle-scroll
               (goog.functions.debounce
                #(>evt [:project/set-scroll-left
                        (-> % .-target .-scrollLeft)])
                100)
               on-scroll
               (fn [e]
                 (.persist e)
                 (handle-scroll e))
               update
               (fn [comp]
                 (let [{:keys [scroll-left]} (r/props comp)]
                   (when (not= (.-scrollLeft @!div) scroll-left)
                     (set! (.-scrollLeft @!div) scroll-left))))]
    (r/create-class
     {:component-did-update
      update
      :reagent-render
      (fn []
        [ui/paper
         {:style {:margin "6px"}}
         [:div {:style {:width "100%"
                        :position "relative"
                        :overflow-x "auto"
                        :font-size 0}
                :ref #(reset! !div %)
                :on-scroll on-scroll
                :on-click (fn [e]
                            (>evt [:project/stop-playback])
                            (state/set-time!
                             (<sub [:project/media-element])
                             (/ (+ (-> e .-currentTarget .-scrollLeft)
                                   (.-pageX e)
                                   -106) ;; TODO: get rid of hardcode
                                (<sub [:project/px-per-sec]))))}
          [ticks]
          [:div {:style {:white-space "nowrap"
                         :display "inline-block"}}
           [tier-labels tiers]
           [tier-rows tiers]]
          [crosshair]]])})))

(defn tiers []
  (let [scroll-left (rf/subscribe [:project/scroll-left])]
    (fn []
      [tiers-inner {:scroll-left @scroll-left}])))
