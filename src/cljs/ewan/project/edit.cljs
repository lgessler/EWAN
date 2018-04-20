(ns ewan.project.edit
  (:require [re-frame.core :as rf]
            [ewan.spec.eaf30 :as eaf30]
            [cljsjs.material-ui]
            [cljs-react-material-ui.core]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as ic]
            [reagent.core :as r]))

(def ^:private default-db {::playback {}
                           ::loaded false})

;; ----------------------------------------------------------------------------
;; subs
;; ----------------------------------------------------------------------------
(rf/reg-sub
 ::loaded
 (fn [db _]
   (::loaded db)))

(rf/reg-sub
 ::current-project
 (fn [db _]
   (::current-project db)))

(rf/reg-sub
 ::playback
 (fn [db _]
   (::playback db)))

;; ----------------------------------------------------------------------------
;; effects
;; ----------------------------------------------------------------------------
;; the video effect is used when a component other than the `video` element
;; itself needs to update the time of the playback. The `video` element's
;; currentTime prop is modified DIRECTLY, and the `on-time-update` event
;; of the video ensures that re-frame will get told about it.
(rf/reg-fx
 :video
 (fn [{:keys [video time]}]
   (set! (.-currentTime video) time)))

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

(defn- video? [t] (= "video" (first (clojure.string/split t #"/"))))
(defn- audio? [t] (= "audio" (first (clojure.string/split t #"/"))))
(defn- media? [t] (or (video? t) (audio? t)))

(defn- videos
  [doc]
  (->> (:_attachments doc)
       (filter (fn [[fname file]] (video? (:content_type file))))
       (map (fn [[fname file]]
              {:src (.createObjectURL js/URL (:data file))
               :play false}))))

(rf/reg-event-db
 ::project-doc-fetched
 (fn [db [_ js-doc]]
   (let [doc (js->clj js-doc :keywordize-keys true)]
     (-> db
         (merge default-db)
         (assoc ::current-project doc)
         (update ::playback merge (first (videos doc)))
         (assoc ::loaded true)))))

;; These events are used by many components to control playback
(rf/reg-event-db
 ::toggle-playback
 (fn [db _]
   (update-in db [::playback :play] not)))

(rf/reg-event-db
 ::time-updated
 (fn [db [_ time]]
   (js/console.log "Time updated: " time)
   (assoc-in db [::playback :time] time)))

(rf/reg-event-fx
 ::set-time
 (fn [{:keys [db]} [_ video time]]
   {:db db
    :video {:video video
            :time time}}))

;; ----------------------------------------------------------------------------
;; views
;; ----------------------------------------------------------------------------


(defn- upper-right-panel []
  [ui/paper {:style {:width "100%"
                     :margin "8px"
                     :padding "8px"}}])

(defn- media-panel-inner []
  (let [!video (atom nil)
        update
        (fn [comp]
          (let [{:keys [play] :as playback} (r/props comp)
                video @!video]
            (cond (and play (.-paused video)) (.play video)
                  (not play) (do
                               (.pause video)
                               (rf/dispatch [::time-updated (.-currentTime video)])))))]
    (r/create-class
     {:component-did-update update
      :component-did-mount (fn [comp]
                             (set! (.-src @!video) (:src (r/props comp)))
                             (update comp))
      :reagent-render
      (fn []
        [:video.project
         {:ref #(reset! !video %)
          :on-click #(rf/dispatch [::toggle-playback])
          :on-time-update #(rf/dispatch [::time-updated (-> % .-target .-currentTime)])}])})))

(defn- media-panel-outer []
  (let [playback (rf/subscribe [::playback])]
    (fn []
      [ui/paper {:style {:width "50%"
                         :max-width "480px"
                         :display "flex"
                         :flex-direction "column"
                         :margin "8px"
                         :padding "8px"}}
       [media-panel-inner @playback]
       [:div.time-container (:time @playback)]])))

(defn- upper-panel []
  [:div.upper-panel
   [media-panel-outer]
   [upper-right-panel]])

(defn- lower-panel []
  [ui/paper {:style {:width "100%"
                     :margin "8px"
                     :padding "8px"}}])

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
