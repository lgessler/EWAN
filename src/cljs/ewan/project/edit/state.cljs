(ns ewan.project.edit.state
  (:require [re-frame.core :as rf]
            [ewan.common :refer [simple-sub]]
            [ewan.spec.eaf30 :as eaf30]
            [cljs.spec.alpha :as s]))

;; project/playback
;;     type, src, play, duration, media-element
;; project/media
;; project/loaded
;; project/px-per-sec
;; project/current-project

(def ^:private default-db {:project/playback {}
                           :project/media (list)
                           :project/loaded false
                           :project/px-per-sec 150
                           :project/scroll-left 0})

;; if you change these, be sure to change the LESS variable as well
(def ^:private TIER_HEIGHT 32)
(def ^:private HALF_TIER_HEIGHT 16)


;; helpers
(defn- zero-pad
  [t]
  (if (>= t 10)
    (str (int t))
    (str "0" (int t))))

(defn time-format
  [t]
  (let [hrs (zero-pad (/ t 3600))
        mins (zero-pad (/ (mod t 3600) 60))
        secs (zero-pad (mod t 60))
        ms (zero-pad (* (mod t 1) 1000))]
    (str hrs ":" mins ":" secs "." ms)))

;; ----------------------------------------------------------------------------
;; effects
;; ----------------------------------------------------------------------------
;; the media effect is used when a component other than the media element
;; itself needs to update the time of the playback. The media element's
;; currentTime prop is modified DIRECTLY, and the `on-time-update` event
;; of the media ensures that re-frame will get told about it.
;;
;; currently unused, but might need in the future
(rf/reg-fx
 :media
 (fn [{:keys [media time type]}]
   (condp = type
     ;; no need for bounds checking--HTMLMediaElement takes care of it
     :add (set! (.-currentTime media) (+ time (.-currentTime media)))
     :set (set! (.-currentTime media) time))))


;; ----------------------------------------------------------------------------
;; events
;; ----------------------------------------------------------------------------
;; These two events are used to fetch a doc when entering #/project/:id
(rf/reg-event-fx
 :project/open-project
 (fn [{:keys [db]} [_ id]]
   {:db db
    :pouchdb {:method "get"
              :args [id
                     {:attachments true
                      :binary true}
                     (fn [err doc]
                       (if err
                         (throw err)
                         (rf/dispatch [:project/project-doc-fetched doc])))]}
    :dispatch [:set-active-panel :project-edit-panel]}))

(defn- video-file? [file]
  (-> file
      :content_type
      (clojure.string/split #"/")
      first
      (= "video")))
(defn- audio-file? [file]
  (-> file
      :content_type
      (clojure.string/split #"/")
      first
      (= "audio")))
(defn- playable-media?
  [file]
  (or (video-file? file) (audio-file? file)))
(defn- playable-media
  "Given a document, returns a seq of maps, where each map
  describes an audio or video file that was attached."
  [doc]
  (->> (:_attachments doc)
       (filter (fn [[_ file]] (playable-media? file)))
       (map (fn [[_ file]]
              {:type (if (video-file? file) :video :audio)
               :src (.createObjectURL js/URL (:data file))
               :play false
               :duration 0
               :media-element nil}))))

(defn- rekeywordize
  "clj->js destroys keyword information, but luckily we can recover it because
  we know we're using hiccup. Used on the :eaf key of a PDB document after
  it is retrieved."
  [hiccup]
  (if-not (vector? hiccup)
    hiccup
    (into [(keyword (first hiccup))]
          (map rekeywordize (rest hiccup)))))

(rf/reg-event-db
 :project/project-doc-fetched
 (fn [db [_ js-doc]]
   (let [doc (update (js->clj js-doc :keywordize-keys true) :eaf rekeywordize)
         file-maps (playable-media doc)]
     (-> db
         (merge default-db)
         (assoc :project/current-project doc)
         (update :project/media into file-maps)
         (update :project/playback merge (or (first (filter #(= (:type %) :video) file-maps))
                                      (first file-maps)))
         (assoc :project/loaded true)))))

;; tests for File objects attached to documents
(rf/reg-event-db
 :project/record-duration
 (fn [db [_ duration]]
   (assoc-in db [:project/playback :duration] duration)))

;; These events are used by many components to control playback
(rf/reg-event-db
 :project/toggle-playback
 (fn [db _]
   (update-in db [:project/playback :play] not)))

(rf/reg-event-db
 :project/time-updated
 (fn [db [_ time]]
   (assoc-in db [:project/playback :time] time)))

(rf/reg-event-db
 :project/set-media-element
 (fn [db [_ elt]]
   (assoc-in db [:project/playback :media-element] elt)))

(rf/reg-event-db
 :project/set-scroll-left
 (fn [db [_ v]]
   (assoc db :project/scroll-left v)))

;; These modify the media element's time directly, in keeping with our
;; convention that the :project/playback map's :time value is only ever
;; set with on-time-update firing from the element.
(defn set-time!
  [elt time]
  (when (and (= time :end) (not (.-paused elt)))
    (rf/dispatch-sync [:project/toggle-playback]))
  (set! (.-currentTime elt)
        (if (= time :end)
          (.-duration elt)
          time)))

(defn add-time!
  [elt time]
  (when (and (>= (+ time (.-currentTime elt)) (.-duration elt))
             (not (.-paused elt)))
    (rf/dispatch-sync [:project/toggle-playback]))
  (set! (.-currentTime elt)
        (+ time (.-currentTime elt))))



;; ----------------------------------------------------------------------------
;; subs
;; ----------------------------------------------------------------------------
(simple-sub :project/loaded)
(simple-sub :project/current-project)
(simple-sub :project/playback)
(simple-sub :project/px-per-sec)
(simple-sub :project/scroll-left)
(simple-sub :project/media-element [:project/playback :media-element])
(simple-sub :project/time [:project/playback :time])
(simple-sub :project/duration [:project/playback :duration])
(simple-sub :project/current-eaf [:project/current-project :eaf])

(rf/reg-sub
 :project/px-per-ms
 :<- [:project/px-per-sec]
 (fn [pps _]
   (/ pps 1000)))

;; individual annotation subs
;; ----------------------------------------------------------------------------
(rf/reg-sub
 :project/ann-times
 :<- [:project/current-eaf]
 (fn [eaf [_ [_ {:keys [annotation-id]}]]]
   (eaf30/get-annotation-times eaf annotation-id)))

(rf/reg-sub
 :project/ann-width
 (fn [[_ ann] _]
   [(rf/subscribe [:project/px-per-ms])
    (rf/subscribe [:project/ann-times ann])])
 (fn [[ppms {:keys [time1 time2]}] [_ [_ {:keys [annotation-id]}]]]
   (* ppms (- time2 time1))))

(rf/reg-sub
 :project/ann-svg-attrs
 (fn [[_ ann] _]
   [(rf/subscribe [:project/px-per-ms])
    (rf/subscribe [:project/ann-times ann])
    (rf/subscribe [:project/ann-width ann])])
 (fn [[ppms {:keys [time1]} width] _]
   {:height TIER_HEIGHT
    :width width
    :x (* ppms time1)
    :y 0}))

(rf/reg-sub
 :project/ann-path-attrs
 (fn [[_ ann] _]
   (rf/subscribe [:project/ann-width ann]))
 (fn [width _]
   {:stroke "black"
    :stroke-width 1
    :d (str "M 0.5 0 l 0 "
            TIER_HEIGHT
            " M 0.5 "
            HALF_TIER_HEIGHT
            " l "
            (- width 1)
            " 0 M "
            (- width 0.5)
            " 0 l 0 "
            TIER_HEIGHT)}))

(rf/reg-sub
 :project/ann-text-attrs
 (fn [db]
   {:x 3
    :y (- HALF_TIER_HEIGHT 2)
    :font-size (- HALF_TIER_HEIGHT 4)
    :style {:user-select "none"}}))

(rf/reg-sub
 :project/ann-text-value
 (fn [db [_ ann]]
   (-> ann (nth 2) (nth 2 nil))))

;; tier subs
;; ----------------------------------------------------------------------------
(rf/reg-sub
 :project/tier-width
 :<- [:project/duration]
 :<- [:project/px-per-sec]
 (fn [[duration pps] _]
   (* duration pps)))

(rf/reg-sub
 :project/tier-height
 (fn [_ _]
   TIER_HEIGHT))

(rf/reg-sub
 :project/tiers
 (fn [db]
   (-> db
       :project/current-project
       :eaf
       eaf30/get-tiers)))

(rf/reg-sub
 :project/crosshair-display-info
 :<- [:project/time]
 :<- [:project/px-per-sec]
 (fn [[time pps] _]
   {:left (* pps time)}))







