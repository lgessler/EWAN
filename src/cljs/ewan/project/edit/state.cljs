(ns ewan.project.edit.state
  (:require [re-frame.core :as rf]
            [ewan.common :refer [simple-sub >evt <sub]]
            [ewan.eaf30.core :as eaf30]
            [cljs.spec.alpha :as s]))


(def ^:private default-db
  {:project/loaded false
   :project/current-project nil ;; loaded from PouchDB during init
   :project/media (list)        ;; list of hash-maps representing playable files
   :project/px-per-sec 150      ;; controls horizontal scale of annotations
   :project/scroll-left 0       ;; the horizontal scroll amount for tiers
   :project/selected-ann-id nil ;; non-nil if selection was formed from ann
   :project/selection-start nil ;; in seconds
   :project/selection-end nil   ;; in seconds
   :project/selected-tier nil
   ;; contains information about the media being played
   ;; child keys: type, src, play, duration, media-element
   :project/playback {}})

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
;; subs
;; ----------------------------------------------------------------------------
(simple-sub :project/loaded)
(simple-sub :project/current-project)
(simple-sub :project/media)
(simple-sub :project/px-per-sec)
(simple-sub :project/scroll-left)
(simple-sub :project/selected-ann-id)
(simple-sub :project/selection-start)
(simple-sub :project/selection-end)
(simple-sub :project/selected-tier)
(simple-sub :project/playback)
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
 :project/ann-begin-time
 (fn [[_ ann] _]
   (rf/subscribe [:project/ann-times ann]))
 (fn [{:keys [time1]} _]
   (/ time1 1000)))

(rf/reg-sub
 :project/ann-path-attrs
 (fn [[_ ann] _]
   (rf/subscribe [:project/ann-width ann]))
 (fn [width [_ ann]]
   {:stroke-width 1
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
 :project/ann-path-color
 :<- [:project/selected-ann-id]
 (fn [selected-id [_ ann]]
   (let [ann-id (-> ann second :annotation-id)]
     {:stroke (if (= selected-id ann-id)
                "blue"
                "black")})))

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

(rf/reg-sub
 :project/selection-duration
 :<- [:project/selection-start]
 :<- [:project/selection-end]
 (fn [[start end] _]
   (- start end)))

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
 :project/tier-svg-width
 :<- [:project/current-eaf]
 (fn [eaf [_ tier-id]]
   (+ 20
      (* 10
         (count (eaf30/get-parent-tiers eaf tier-id))))))

(rf/reg-sub
 :project/is-parent-tier
 :<- [:project/current-eaf]
 (fn [eaf [_ tier-id]]
   (eaf30/is-parent-tier eaf tier-id)))

(rf/reg-sub
 :project/is-selected-tier
 :<- [:project/selected-tier]
 (fn [selected-id [_ tier-id]]
   (= selected-id tier-id)))

;; crosshair and selection subs
;; ----------------------------------------------------------------------------
(rf/reg-sub
 :project/crosshair-display-info
 :<- [:project/time]
 :<- [:project/px-per-sec]
 (fn [[time pps] _]
   {:left (* pps time)}))

(rf/reg-sub
 :project/selection-display-info
 :<- [:project/px-per-sec]
 :<- [:project/selection-start]
 :<- [:project/selection-end]
 (fn [[pps start end] _]
   {:left (* pps start)
    :width (* pps (- end start))}))

;; playback button subs
(rf/reg-sub
 :project/playable-selection?
 :<- [:project/selection-start]
 :<- [:project/selection-end]
 (fn [[start end] _]
   (and start end (> (- end start) 0))))

;; ----------------------------------------------------------------------------
;; events
;; ----------------------------------------------------------------------------

;; init
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
         (update :project/playback
                 merge
                 (or (first (filter #(= (:type %) :video) file-maps))
                     (first file-maps)))
         (assoc :project/loaded true)))))

;; used to note MediaElement.duration once it has loaded
(rf/reg-event-db
 :project/record-duration
 (fn [db [_ duration]]
   (assoc-in db [:project/playback :duration] duration)))

;; store a ref to the media element
(rf/reg-event-db
 :project/set-media-element
 (fn [db [_ elt]]
   (assoc-in db [:project/playback :media-element] elt)))

;; scaling
;; ---------------------------------------------------------------------------
(rf/reg-event-db
 :project/incr-px-per-sec
 (fn [db _]
   (assoc db :project/px-per-sec (min (+ (:project/px-per-sec db) 25) 300))))

(rf/reg-event-db
 :project/decr-px-per-sec
 (fn [db _]
   (assoc db :project/px-per-sec (max (- (:project/px-per-sec db) 25) 75))))

;; time updates
;; ---------------------------------------------------------------------------

;; :project/time-updated is an event fired by the HTMLMediaElement. We need a
;; cofx called :tier-visual-width in order to get the width of the client's
;; viewport
(defn- find-left-offset
  "Find the left offset of an element relative to the document recursively"
  ([e]
   (find-left-offset 0 e))
  ([acc e]
   (if (and e (.-offsetLeft e))
     (recur (+ (.-offsetLeft e) acc) (.-offsetParent e))
     acc)))
(rf/reg-cofx
 :tier-visual-width
 (fn [cofx _]
   (let [viewport-width (.-innerWidth js/window)
         left (find-left-offset
               (.querySelector js/document ".tier-rows"))]
     (assoc cofx :tier-visual-width (- viewport-width left)))))
(rf/reg-event-fx
 :project/time-updated
 [(rf/inject-cofx :tier-visual-width)]
 (fn [{:keys [db tier-visual-width]} [_ time]]
   (let [pps (:project/px-per-sec db)
         time-in-px (* time pps)
         scroll-left (:project/scroll-left db)
         playing (get-in db [:project/playback :play])
         left-bound scroll-left
         right-bound (- (+ scroll-left tier-visual-width) 30) ;; vertical scrollbar takes some space
         right-bound-while-playing (- right-bound 70)]
     {:db
      (cond-> db
        true
        (assoc-in [:project/playback :time] time)
        (and playing (> time-in-px right-bound-while-playing))
        (assoc :project/scroll-left (- time-in-px 100))
        ;; we don't do bounds checking on these, so it's possible they'll get
        ;; set to a value outside of the interval [0, width], but this is OK
        ;; since :project/scroll-left's only purpose is to set the scroll left
        ;; attr on the tier dom element, which accepts values outside the
        ;; interval without harm
        (> time-in-px right-bound)
        (assoc :project/scroll-left (+ (- time-in-px right-bound) left-bound))
        (< time-in-px left-bound)
        (assoc :project/scroll-left (- time-in-px 20)))})))

;; These are not events--they modify the media element's time directly,
;; in keeping with our  convention that the :project/playback map's :time
;; value is only ever set with on-time-update firing from the element.
(defn set-time!
  [elt time]
  (set! (.-currentTime elt)
        (if (= time :end)
          (.-duration elt)
          time)))

(defn add-time!
  [elt time]
  (set! (.-currentTime elt)
        (+ time (.-currentTime elt))))

;; playback events
;; ---------------------------------------------------------------------------
(rf/reg-event-db
 :project/toggle-playback
 (fn [db _]
   (update-in db [:project/playback :play] not)))

(rf/reg-event-db
 :project/start-playback
 (fn [db _]
   (assoc-in db [:project/playback :play] true)))

(rf/reg-event-db
 :project/stop-playback
 (fn [db _]
   (assoc-in db [:project/playback :play] false)))

;; scroll and selection events
;; ---------------------------------------------------------------------------
(rf/reg-event-db
 :project/set-scroll-left
 (fn [db [_ s]]
   (assoc db :project/scroll-left s)))

(rf/reg-event-db
 :project/clear-selection
 (fn [db _]
   (-> db
       (assoc :project/selection-start nil)
       (assoc :project/selection-end nil)
       (assoc :project/selected-ann-id nil))))

(rf/reg-event-db
 :project/select-ann
 (fn [db [_ id]]
   (let [hiccup (get-in db [:project/current-project :eaf])
         {:keys [time1 time2]} (eaf30/get-annotation-times hiccup id)]
     (-> db
         (assoc :project/selection-start (/ time1 1000))
         (assoc :project/selection-end (/ time2 1000))
         (assoc :project/selected-ann-id id)))))

(rf/reg-event-db
 :project/set-selection
 (fn [db [_ start end]]
   (cond-> db
     (some? start) (assoc :project/selection-start start)
     (some? end) (assoc :project/selection-end end))))

(rf/reg-event-db
 :project/select-tier
 (fn [db [_ tier-id]]
   (assoc db :project/selected-tier tier-id)))

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
