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

;; ----------------------------------------------------------------------------
;; effects
;; ----------------------------------------------------------------------------
;; the media effect is used when a component other than the media element
;; itself needs to update the time of the playback. The media element's
;; currentTime prop is modified DIRECTLY, and the `on-time-update` event
;; of the media ensures that re-frame will get told about it.
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

(rf/reg-event-db
 ::project-doc-fetched
 (fn [db [_ js-doc]]
   (let [doc (js->clj js-doc :keywordize-keys true)
         file-maps (playable-media doc)]
     (-> db
         (merge default-db)
         (assoc ::current-project doc)
         (update ::media into file-maps)
         (update ::playback merge (first file-maps))
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

(rf/reg-event-fx
 ::set-time
 (fn [{:keys [db]} [_ time]]
   (let [elt (-> db ::playback :media-element)
         time (if (= time :end) (.-duration elt) time)]
     {:db db
      :media {:media elt
              :type :set
              :time time}})))

(rf/reg-event-fx
 ::add-time
 (fn [{:keys [db]} [_ time]]
   {:db db
    :media {:media (-> db ::playback :media-element)
            :type :add
            :time time}}))

(rf/reg-event-db
 ::test
 (fn [db _]
   (assoc db ::playback (rand-nth (::media db)))))

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
            ;; that to work very well when I tried. Global `def` is even worse.
            ;; This'll do for now.
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
            :on-click #(rf/dispatch [::test])
            :on-time-update
            #(rf/dispatch [::time-updated (-> % .-target .-currentTime)])}]
          [:div "Audio"]))})))

(defn- time-container [playback]
  [:div.media-panel__time-container (:time @playback)])


(defn- playback-button
  [{:keys [on-click icon-name]}]
  [ui/icon-button {:icon-class-name "material-icons"
                   :icon-style {:width "24px" :height "24px"}
                   :style {:width "36px" :height "36px" :padding "6px"}
                   :on-click on-click}
   icon-name])
(defn- playback-buttons [playback]
  [:div.media-panel__playback-buttons
   [playback-button {:on-click #(rf/dispatch [::set-time 0])
                     :icon-name "first_page"}]
   [playback-button {:on-click #(rf/dispatch [::add-time -5])
                     :icon-name "replay_5"}]
   [playback-button {:on-click #(rf/dispatch [::add-time -0.02])
                     :icon-name "navigate_before"}]
   [playback-button {:on-click #(rf/dispatch [::toggle-playback])
                     :icon-name (if (:play @playback) "pause" "play_arrow")}]
   [playback-button {:on-click #(rf/dispatch [::add-time 0.02])
                     :icon-name "navigate_next"}]
   [playback-button {:on-click #(rf/dispatch [::add-time 5])
                     :icon-name "forward_5"}]
   [playback-button {:on-click #(rf/dispatch [::set-time :end])
                     :icon-name "last_page"}]
   ])

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
