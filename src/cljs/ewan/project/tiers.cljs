(ns ewan.project.tiers
  (:require [re-frame.core :as rf]
            [ewan.spec.eaf30 :as eaf30]
            [cljsjs.material-ui]
            [cljs-react-material-ui.core]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as ic]
            [reagent.core :as r]))

(def default-db {::px-per-sec 150})

;; if you change these, be sure to change the LESS variable as well
(def ^:private TIER_HEIGHT 32)
(def ^:private HALF_TIER_HEIGHT 16)


;; ----------------------------------------------------------------------------
;; subs
;; ----------------------------------------------------------------------------
(rf/reg-sub ::px-per-sec (fn [db] (::px-per-sec db)))
(rf/reg-sub ::time
            (fn [db] (get-in db [:ewan.project.edit/playback :time])))
(rf/reg-sub ::duration
            (fn [db] (get-in db [:ewan.project.edit/playback :duration])))

(rf/reg-sub
 ::time-vals
 (fn [db [_ ann]]
   (eaf30/get-annotation-times (-> db
                                   :ewan.project.edit/current-project
                                   :eaf)
                               (-> ann second :annotation-id))))

(rf/reg-sub
 ::tiers
 (fn [db]
   (-> db
       :ewan.project.edit/current-project
       :eaf
       eaf30/get-tiers)))

(defn- alignable-annotation
  [a-ann]
  ;; TODO: find out why time-slot-ref1 and ref2 are sometimes allowed
  ;; to refer to time-slots without a value
  (let [pps @(rf/subscribe [::px-per-sec])
        ppms (/ pps 1000)
        {:keys [time1 time2]} @(rf/subscribe [::time-vals a-ann])
        x (* ppms time1)
        width (* ppms (- time2 time1))]
    [:svg {:height TIER_HEIGHT :width width :x x :y 0}
     [:path {:stroke "black" :stroke-width 1
             :d (str "M 0.5 0 l 0 "
                     TIER_HEIGHT
                     " M 0.5 "
                     HALF_TIER_HEIGHT
                     " l "
                     (- width 1)
                     " 0 M "
                     (- width 0.5)
                     " 0 l 0 "
                     TIER_HEIGHT)}]
     [:text {:x 3 :y 14 :font-size 12}
      (-> a-ann (nth 2) (nth 2 nil))]]))

(def ^:private ref-annotation alignable-annotation)

(defn- annotation
  [ann]
  (let [ann-type (-> ann (nth 2) first)]
    (condp = ann-type
      :alignable-annotation [alignable-annotation (nth ann 2)]
      :ref-annotation [ref-annotation (nth ann 2)]
      nil)))

(defn- tier-row
  [tier]
  (let [duration (rf/subscribe [::duration])
        pps (rf/subscribe [::px-per-sec])
        w (* @duration @pps)]
    [:div.tier-rows__row
     [:svg {:width w :height TIER_HEIGHT}
      (for [ann (drop 2 tier)]
        ^{:key (-> ann (nth 2) second :annotation-id)} [annotation ann])]]))

(defn- tier-rows [tiers]
    [:div.tier-rows__container
     {:on-click #(ewan.project.edit/set-time!
                  @(rf/subscribe [:ewan.project.edit/media-element])
                  (-> % .-pageX (- 106) (/ @(rf/subscribe [::px-per-sec]))))}
     ;; 106 = 100 (label width) + 6 (margin on the top-level paper element)
     (for [tier @tiers]
       ^{:key (-> tier second :tier-id)} [tier-row tier])])

(defn- tier-labels [tiers]
  [:div.tier-labels__container
   (for [tier @tiers]
     (let [tier-id (-> tier second :tier-id)]
       ^{:key tier-id} [:div.tier-labels__row tier-id]))])

(defn- crosshair []
  (let [time (rf/subscribe [::time])
        pps (rf/subscribe [::px-per-sec])]
    [:div.crosshair {:style {:left (str (+ 100 (* @pps @time)) "px")}}]))






(defn- ticks []
  (let [pps (rf/subscribe [::px-per-sec])
        duration (rf/subscribe [::duration])]
    [:div.ticks__container
     [:div.ticks__spacer]
     [:svg.ticks {:width (* @pps @duration)}
      ;; use decisecs to avoid float precision issues
      (for [decisec (range 0 (+ 10 (* @duration 10)))]
        (let [sec (/ decisec 10)
              x (* sec @pps)]
          (if (= (mod decisec 10) 0)
            [:g {:key decisec}
             [:line {:x1 x :x2 x :y1 0 :y2 6 :stroke-width 0.5 :stroke "black"}]
             [:text {:x x :y 16 :font-size 10 :text-anchor "middle"}
              (ewan.project.edit/time-format sec)]]
            [:line {:key decisec :x1 x :x2 x :y1 0 :y2 3 :stroke-width 0.5 :stroke "black"}]
            )))]]))

(defn tiers []
  (let [tiers (rf/subscribe [::tiers])
        rows (atom nil)]
    (fn []
      [ui/paper
       {:style {:margin "6px"}}
       [:div {:style {:width "100%"
                      :position "relative"
                      :overflow-x "auto"
                      :font-size 0}}
        [ticks]
        [:div {:style {:white-space "nowrap"
                       :display "inline-block"}}
         [tier-labels tiers]
         [tier-rows tiers]]
        [crosshair]]])))

