(ns ewan.project.tiers
  (:require [re-frame.core :as rf]
            [ewan.spec.eaf30 :as eaf30]
            [cljsjs.material-ui]
            [cljs-react-material-ui.core]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as ic]
            [reagent.core :as r]))

(def default-db {::px-per-sec 150})


;; ----------------------------------------------------------------------------
;; subs
;; ----------------------------------------------------------------------------
(rf/reg-sub ::px-per-sec (fn [db] (::px-per-sec db)))
(rf/reg-sub ::time
            (fn [db] (get-in db [:ewan.project.edit/playback :time])))
(rf/reg-sub ::duration
            (fn [db] (get-in db [:ewan.project.edit/playback :duration])))

(rf/reg-sub
 ::eaf
 (fn [db]
   (-> db
       :ewan.project.edit/current-project
       :eaf)))

(rf/reg-sub
 ::tiers
 (fn [db]
   (-> db
       :ewan.project.edit/current-project
       :eaf
       eaf30/get-tiers)))

(defn- alignable-annotation
  [a-ann pps eaf]
  (let [ppms (/ pps 1000)
        {:keys [time-slot-ref1 time-slot-ref2] :as attrs} (second a-ann)
        start (eaf30/get-time-slot-val @eaf time-slot-ref1)
        end (eaf30/get-time-slot-val @eaf time-slot-ref2)
        x (* ppms start)
        width (* ppms (- end start))
        width (if (< x 0) 20 width)] ;; TODO: find out what it means for a time-slot to not have a val
    [:svg {:height 32 :width width :x x :y 0}
     [:path {:stroke "black" :stroke-width 1
             :d (str "M 0.5 0 l 0 32 M 0.5 16 l " (- width 1) " 0 M " (- width 0.5) " 0 l 0 32")}]
     [:text {:x 3 :y 14 :font-size 12}
      (-> a-ann (nth 2) (nth 2))]
     ]))

(defn- annotation
  [ann pps eaf]
  (let [ann-type (-> ann (nth 2) first)]
    (condp = ann-type
      :alignable-annotation [alignable-annotation (nth ann 2) pps eaf]
      nil)))

(defn- tier-row
  [tier duration pps eaf]
  (let [w (* @duration @pps)]
    [:div.tier-rows__row
     [:svg {:width w :height "32"}
      (for [ann (drop 2 tier)]
        ^{:key (-> ann (nth 2) second :annotation-id)} [annotation ann @pps eaf])]]))

(defn- tier-rows [tiers]
  (let [pps (rf/subscribe [::px-per-sec])
        duration (rf/subscribe [::duration])
        eaf (rf/subscribe [::eaf])]
    [:div.tier-rows__container
     (for [tier @tiers]
       ^{:key (-> tier second :tier-id)} [tier-row tier duration pps eaf])]))

(defn- tier-labels [tiers]
  [:div.tier-labels__container
   (for [tier @tiers]
     (let [tier-id (-> tier second :tier-id)]
       ^{:key tier-id} [:div.tier-labels__row tier-id]))])

(defn- crosshair []
  (let [time (rf/subscribe [::time])
        pps (rf/subscribe [::px-per-sec])]
    [:div.crosshair {:style {:left (str (+ 100 (* @pps @time)) "px")}}]))

(defn tiers []
  (let [tiers (rf/subscribe [::tiers])
        rows (atom nil)]
    (fn []
      [ui/paper
       {:style {:margin "6px"}}
       [:div {:style {:width "100%"
                      :position "relative"
                      :overflow-x "auto"
                      :white-space "nowrap"}}
        [tier-labels tiers]
        [tier-rows tiers]
        [crosshair]]])))

