(ns ewan.project.edit.tiers
  (:require [re-frame.core :as rf]
            [cljsjs.material-ui]
            [cljs-react-material-ui.core]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as ic]
            [ewan.common :refer [tag-name attrs first-child children <sub >evt]]
            [ewan.project.edit.state :as state]
            [ewan.project.edit.annotation-edit-dialog :refer [annotation-edit-dialog]]
            [reagent.core :as r]
            [goog.functions]
            [ewan.eaf30.core :as eaf]
            [ewan.eaf30.core :as eaf30]))

;; keep in sync with @tier-label-width in less
(def ^:private LABEL_WIDTH 100)
(def ^:private MARGIN 6)
(def ^:private TIER_CONTENT_OFFSET (+ MARGIN LABEL_WIDTH))

(defn- alignable-annotation
  [a-ann]
  ;; TODO: find out why time-slot-ref1 and ref2 are sometimes allowed
  ;; to refer to time-slots without a value
  (let [elt (<sub [:project/media-element])
        t (<sub [:project/ann-begin-time a-ann])
        id (-> a-ann attrs :annotation-id)
        drags (atom 0)]
    [:svg (merge (<sub [:project/ann-svg-attrs a-ann])
                 {:style {:pointer-events "bounding-box"}
                  :on-mouse-move (fn [e]
                                   (when (= (.-buttons e) 1)
                                     (swap! drags + 1)))
                  :on-click (fn [e]
                              (when-not (> @drags 3)
                                (.stopPropagation e)
                                (rf/dispatch [:project/stop-playback])
                                (rf/dispatch [:project/select-ann id])
                                (state/set-time! elt t))
                              (reset! drags 0))
                  :on-double-click (fn [e]
                                     (.stopPropagation e) ;; don't send to tier
                                     (>evt [:project/open-ann-edit-dialog {:ann-id id}]))})
     [:path (merge (<sub [:project/ann-path-attrs a-ann])
                   (<sub [:project/ann-path-color a-ann]))]
     [:text (<sub [:project/ann-text-attrs])
      (<sub [:project/ann-text-value a-ann])]]))

;; for now, there doesn't seem to be a reason to treat them differently
(def ^:private ref-annotation alignable-annotation)

;; Argument looks something like this:
;; [:annotation {} ;; <-- always empty
;;  [:alignable-annotation {...} ;; <-- has attrs
;;   [:annotation-value {}
;;    ...]]]
;; (Yes, the outer hiccup node doesn't appear to serve a purpose)
(defn- annotation
  [[_ _ [ann-type _ _ :as inner-ann]]]
  (condp = ann-type
    :alignable-annotation [alignable-annotation inner-ann]
    :ref-annotation [ref-annotation inner-ann]
    nil))

(defn- tier-row
  [[_ {:keys [tier-id]} & annotations]]
  [:div.tier-row
   [:svg {:width (<sub [:project/tier-width])
          :height (<sub [:project/tier-height])
          :on-double-click #(if (<sub [:project/tier-has-constraint tier-id])
                              (js/alert "Sorry, annotation creation on tiers with constraints isn't supported yet.")
                              (>evt [:project/open-ann-edit-dialog {:tier-id tier-id}]))}
    (doall
     (for [ann annotations]
       ^{:key (-> ann first-child attrs :annotation-id)}
       [annotation ann]))]])

(defn- tier-rows [tiers]
  (let [pps (<sub [:project/px-per-sec])
        x-to-sec #(/ (- % TIER_CONTENT_OFFSET) pps)
        offset (<sub [:project/scroll-left])
        start-time (atom nil)]
    [:div.tier-rows
     ;; forming selections by dragging is handled here
     ;; ELAN has more sophisticated control of selection forming
     ;; for aligned tiers, but this is currently not implemented.
     ;; Handling of right-to-left drag (which would seem to make
     ;; a negative selection) is handled in the re-frame event
     ;; handlers.
     {:on-mouse-down
      (fn [e]
        (reset! start-time nil))
      :on-mouse-move
      (fn [e]
        (.stopPropagation e)
        (when (= (.-buttons e) 1)
          (let [t (x-to-sec (+ offset (.-pageX e)))]
            (state/set-time! (<sub [:project/media-element]) t)
            (if-not @start-time
              (do
                (>evt [:project/set-selection t t])
                (reset! start-time t))
              (if (>= t @start-time)
                (>evt [:project/set-selection @start-time t])
                (>evt [:project/set-selection t @start-time]))))))
      :on-mouse-up
      (fn [e]
        (when @start-time
          (.stopPropagation e)))}
     (doall
      (for [tier @tiers]
        ^{:key (-> tier attrs :tier-id)}
        [tier-row tier]))]))

(defn- tier-label [tier-id]
  (let [th (<sub [:project/tier-height])
        half-th (/ th 2)
        tw (<sub [:project/tier-svg-width tier-id])
        left (- tw 10)
        parent-tier (<sub [:project/is-parent-tier tier-id])
        is-selected (<sub [:project/is-selected-tier tier-id])]
    [:div.tier-label
     {:on-double-click
      (fn [e]
        (>evt [:project/select-tier tier-id]))}
     [:svg.tier-label__pipe
      {:width tw :height th
       :view-box (str "0 0 " tw " " th)}
      [:circle {:cx left :cy half-th
                :r 3 :fill (if is-selected "red" "white")
                :stroke (if is-selected "red" "black")}]
      ;; TODO: more helpful pipe structures
      ;(doall
      ; (for [x (range left 0 -10)]
      ;   ^{:key x}
      ;   [:line {:x1 x :y1 0 :x2 x :y2 th}]))
      ;(when parent-tier
      ;  [:line {:x1 left :y1 th :x2 (+ left 10) :y2 th}])
      ;[:line {:x1 left :x2 (+ left 8)
      ;        :y1 (/ th 2) :y2 (/ th 2)}]
      ]
     [:div.tier-label__text
      {:style {:font-weight (if is-selected "bold" "normal")
               :color (if is-selected "red" "black")}}
      tier-id]]))

(defn- add-tier-button []
  [:div.tier-label--add-button
   [ui/flat-button {:style {:width "100%"
                            :height "100%"}
                    :hover-color "#d3ffea"
                    :ripple-color "#00ff84"
                    :icon (r/as-element
                           [ui/font-icon {:class-name "material-icons"}
                            "add"])
                    :on-click #(js/alert (str "Sorry, adding tiers isn't currently supported."
                                              " Please make your tiers in an ELAN file and "
                                              "upload your project again."))}]])

(defn- tier-labels [tiers]
  [:div.tier-labels
   {:on-click #(.stopPropagation %)}
   (doall
    (for [tier @tiers]
      (let [tier-id (-> tier attrs :tier-id)]
        ^{:key tier-id}
        [tier-label tier-id])))
   [add-tier-button]])

(defn- crosshair []
  "A line aligned with a certain time that indicates
   the current point in the playback of the media"
  (fn []
    (let [{:keys [left]} (<sub [:project/crosshair-display-info])]
      [:div.crosshair {:style {:left (str (+ LABEL_WIDTH left) "px")}}])))

(defn- selection []
  (fn []
    (let [{:keys [left width]} (<sub [:project/selection-display-info])]
      [:div.selection {:style {:left (str (+ LABEL_WIDTH left) "px")
                               :width (str width "px")}}])))

(defn- decrease-pps-button []
  [ui/icon-button
   {:icon-class-name "material-icons"
    :icon-style {:width "18px" :height "18px" :color "inherit"}
    :style {:width "24px" :height "24px" :padding "3px" :color "#bbb"}
    :hovered-style {:color "black"}
    :on-click (fn [e]
                (.stopPropagation e)
                (>evt [:project/decr-px-per-sec]))}
   "zoom_out"])

(defn- increase-pps-button []
  [ui/icon-button
   {:icon-class-name "material-icons"
    :icon-style {:width "18px" :height "18px" :color "inherit"}
    :style {:width "24px" :height "24px" :padding "3px" :color "#bbb"}
    :hovered-style {:color "black"}
    :on-click (fn [e]
                (.stopPropagation e)
                (>evt [:project/incr-px-per-sec]))}
   "zoom_in"])

(defn- ticks []
  "A thin strip with a tick at every decisecond and a timestamp at every second. Also
   includes buttons for increasing or decreasing pixels per second"
  (let [pps (rf/subscribe [:project/px-per-sec])
        duration (rf/subscribe [:project/duration])]
    [:div.ticks-row
     [:div.ticks__spacer ;; provides horizontal space in addition to holding the buttons
      [decrease-pps-button]
      [increase-pps-button]]
     [:svg.ticks {:width (* @pps @duration)}
      ;; use decisecs to avoid float precision issues
      (doall
       (for [decisec (range 0 (+ 10 (* @duration 10)))]
         (let [sec (/ decisec 10)
               x (* sec @pps)]
           (if (= (mod decisec 10) 0)
             [:g {:key decisec}
              [:line {:x1 x :x2 x :y1 0 :y2 6 :stroke-width 0.5 :stroke "black"}]
              [:text {:x x :y 16 :font-size 10
                      :text-anchor "middle"
                      :style {:user-select "none"}}
               (state/time-format sec)]]
             [:line {:key decisec :x1 x :x2 x :y1 0 :y2 3
                     :stroke-width 0.5 :stroke "black"}]))))]]))

(defn- tiers-inner
  "We need sophisticated handling to allow control of this div's scrollLeft property
   from the re-frame database's variable for it. Whenever :project/scroll-left is
   updated, this reagent component's did-update event is fired"
  []
  (r/with-let
    [tiers (rf/subscribe [:project/tiers])
     !div (atom nil)
     ;; create just one instance of the function that will inform the DB of scroll
     ;; position and debounce it so it will not fire too often (scroll events fire
     ;; very quickly)
     handle-scroll (goog.functions.debounce
                    #(>evt [:project/set-scroll-left
                            (-> % .-target .-scrollLeft)])
                    100)
     on-scroll (fn [e]
                 (.persist e)
                 (handle-scroll e))
     update (fn [comp]
              (let [{:keys [scroll-left]} (r/props comp)]
                (when (not= (.-scrollLeft @!div) scroll-left)
                  (set! (.-scrollLeft @!div) scroll-left))))]
    (r/create-class
     {:component-did-update
      update
      :reagent-render
      (fn []
        [ui/paper
         {:style {:margin (str MARGIN "px")
                  :min-height "100%"}}
         [:div.tiers
          {:ref #(reset! !div %)
           :on-scroll on-scroll
           :on-click (fn [e]
                       (>evt [:project/stop-playback])
                       (state/set-time!
                        (<sub [:project/media-element])
                        (/ (+ (-> e .-currentTarget .-scrollLeft)
                              (.-pageX e)
                              (- TIER_CONTENT_OFFSET))
                           (<sub [:project/px-per-sec]))))}
          [ticks]
          [:div {:style {:white-space "nowrap"
                         :display "inline-block"}}
           [tier-labels tiers]
           [tier-rows tiers]]
          [selection]
          [crosshair]]])})))

(defn tiers []
  "See https://github.com/Day8/re-frame/blob/master/docs/Using-Stateful-JS-Components.md"
  (let [scroll-left (rf/subscribe [:project/scroll-left])]
    (fn []
      [:div
       [tiers-inner {:scroll-left @scroll-left}]
       [annotation-edit-dialog]])))
