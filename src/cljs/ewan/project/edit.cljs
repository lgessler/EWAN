(ns ewan.project.edit
  (:require [re-frame.core :as rf]
            [ewan.spec.eaf30 :as eaf30]
            [cljsjs.material-ui]
            [cljs-react-material-ui.core]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as ic]
            [reagent.core :as r]))

(def ^:private default-db {::playback {}
                           ::media (list)
                           ::loaded false})

;; ----------------------------------------------------------------------------
;; subs
;; ----------------------------------------------------------------------------
(rf/reg-sub ::loaded (fn [db _] (::loaded db)))
(rf/reg-sub ::current-project (fn [db _] (::current-project db)))
(rf/reg-sub ::playback (fn [db _] (::playback db)))
(rf/reg-sub ::media-element (fn [db _] (:media-element (::playback db))))

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
 ::open-project
 (fn [{:keys [db]} [_ id]]
   {:db db
    :pouchdb {:method "get"
              :args [id
                     {:attachments true
                      :binary true}
                     (fn [err doc]
                       (if err
                         (throw err)
                         (rf/dispatch [::project-doc-fetched doc])))]}
    :dispatch [:ewan.views/set-active-panel :project-edit-panel]}))

;; tests for File objects attached to documents
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
               :play false}))))

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
 ::project-doc-fetched
 (fn [db [_ js-doc]]
   (let [doc (update (js->clj js-doc :keywordize-keys true) :eaf rekeywordize)
         file-maps (playable-media doc)]
     (-> db
         (merge default-db)
         (assoc ::current-project doc)
         (update ::media into file-maps)
         (update ::playback merge (or (first (filter #(= (:type %) :video) file-maps))
                                      (first file-maps)))
         (assoc ::loaded true)))))

;; These events are used by many components to control playback
(rf/reg-event-db
 ::toggle-playback
 (fn [db _]
   (update-in db [::playback :play] not)))

(rf/reg-event-db
 ::time-updated
 (fn [db [_ time]]
   (assoc-in db [::playback :time] time)))

(rf/reg-event-db
 ::set-media-element
 (fn [db [_ elt]]
   (assoc-in db [::playback :media-element] elt)))

;; These modify the media element's time directly, in keeping with our
;; convention that the ::playback map's :time value is only ever
;; set with on-time-update firing from the element.
(defn- set-time!
  [elt time]
  (when (and (= time :end) (not (.-paused elt)))
    (rf/dispatch-sync [::toggle-playback]))
  (set! (.-currentTime elt)
        (if (= time :end)
          (.-duration elt)
          time)))

(defn- add-time!
  [elt time]
  (when (and (>= (+ time (.-currentTime elt)) (.-duration elt))
             (not (.-paused elt)))
    (rf/dispatch-sync [::toggle-playback]))
  (set! (.-currentTime elt)
        (+ time (.-currentTime elt))))

;; ----------------------------------------------------------------------------
;; views
;; ----------------------------------------------------------------------------

;; media ----------------------------------------------------------------------
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
           {:ref #(rf/dispatch-sync [::set-media-element %])
            :on-time-update
            #(rf/dispatch [::time-updated (-> % .-target .-currentTime)])}]
          [:div "Audio"]))})))

(defn- zero-pad
  [t]
  (if (>= t 10)
    (str (int t))
    (str "0" (int t))))

(defn- time-format
  [t]
  (let [hrs (zero-pad (/ t 3600))
        mins (zero-pad (/ (mod t 3600) 60))
        secs (zero-pad (mod t 60))
        centisecs (zero-pad (* (mod t 1) 100))]
    (str hrs ":" mins ":" secs "." centisecs)))

(defn- time-container [playback]
  [:div.media-panel__time-container
   (time-format (:time @playback))])


(defn- playback-button
  [{:keys [on-click icon-name]}]
  [ui/icon-button {:icon-class-name "material-icons"
                   :icon-style {:width "24px" :height "24px"}
                   :style {:width "36px" :height "36px" :padding "6px"}
                   :on-click on-click}
   icon-name])
(defn- playback-buttons [playback]
  (let [elt (rf/subscribe [::media-element])]
    [:div.media-panel__playback-buttons
     [playback-button {:on-click #(set-time! @elt 0)
                       :icon-name "first_page"}]
     [playback-button {:on-click #(add-time! @elt -5)
                       :icon-name "replay_5"}]
     [playback-button {:on-click #(add-time! @elt -0.02)
                       :icon-name "navigate_before"}]
     [playback-button {:on-click #(rf/dispatch [::toggle-playback])
                       :icon-name (if (:play @playback) "pause" "play_arrow")}]
     [playback-button {:on-click #(add-time! @elt 0.02)
                       :icon-name "navigate_next"}]
     [playback-button {:on-click #(add-time! @elt 5)
                       :icon-name "forward_5"}]
     [playback-button {:on-click #(set-time! @elt :end)
                       :icon-name "last_page"}]
     ]))

(defn- media-panel-outer []
  (let [playback (rf/subscribe [::playback])]
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

;; lower panel ----------------------------------------------------------------
(defn- lower-panel []
  [ui/paper {:style {:width "100%"
                     :margin "6px"
                     :padding "8px"}}])

;; root element ---------------------------------------------------------------
(defn project-edit-panel-body []
  (r/with-let [doc (rf/subscribe [::current-project])
               loaded (rf/subscribe [::loaded])]
    (if @loaded
      [:div
       [upper-panel]
       [lower-panel]]
      [:div.page-loading
       [ui/circular-progress {:size 80
                              :thickness 7}]])))
