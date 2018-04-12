(ns ewan.project.edit
  (:require [re-frame.core :as rf]
            [ewan.spec.eaf30 :as eaf30]
            [cljsjs.material-ui]
            [cljs-react-material-ui.core]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as ic]
            [reagent.core :as r]))

(def ^:private default-db {::playback {}})

;; ----------------------------------------------------------------------------
;; subs
;; ----------------------------------------------------------------------------
(rf/reg-sub
 ::current-project
 (fn [db _]
   (::current-project db)))

(rf/reg-sub
 ::playback
 (fn [db _]
   (::playback db)))

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
    :dispatch-n [[:ewan.views/set-active-panel :project-edit-panel]
                 [::init-db]]}))

(rf/reg-event-db
 ::project-doc-fetched
 (fn [db [_ doc]]
   (assoc db ::current-project (js->clj doc :keywordize-keys true))))

(rf/reg-event-db
 ::init-db
 (fn [db _]
   (merge db default-db)))

;; ----------------------------------------------------------------------------
;; views
;; ----------------------------------------------------------------------------

(defn- video? [t]
  (= "video" (first (clojure.string/split t #"/"))))

(defn- media-panel-inner []
  (let [!video (atom nil)
        update
        (fn [comp]
          (let [{:keys [src play] :as playback} (r/props comp)
                video @!video]
            (js/console.log playback)
            (set! (.-src video) src)
            (if play
              (.play video)
              (.pause video))))]
    (r/create-class
     {:component-did-update update
      :component-did-mount update
      :reagent-render
      (fn []
        [:video {:ref #(reset! !video %)
                 :width "480"
                 :height "640"}])})))

(defn- media-panel-outer []
  (let [playback (rf/subscribe [::playback])]
    (fn []
      [media-panel-inner @playback])))

(defn- video-panel []
  (let [!ref (atom nil)]
    (fn []
      [:video {:ref (fn [com] (reset! !ref com))}])))

(defn project-edit-panel-body [doc]
  (r/with-let [doc (rf/subscribe [::current-project])]
    [:div (for [[_ file] (:_attachments @doc)]
            (when (video? (:content_type file))
              [:video {:src (.createObjectURL js/URL (:data file))
                       :loop "loop"}]))]))
