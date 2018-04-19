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
    :dispatch [:ewan.views/set-active-panel :project-edit-panel]}))

(defn- video? [t]
  (= "video" (first (clojure.string/split t #"/"))))

(defn- videos
  [doc]
  (->> (:_attachments doc)
       (filter (fn [[fname file]] (video? (:content_type file))))
       (map (fn [[fname file]]
              {:src (.createObjectURL js/URL (:data file))
               :play true}))))

(rf/reg-event-db
 ::project-doc-fetched
 (fn [db [_ js-doc]]
   (let [doc (js->clj js-doc :keywordize-keys true)]
     (-> db
         (merge default-db)
         (assoc ::current-project doc)
         (update ::playback merge (first (videos doc)))))))

(rf/reg-event-db
 ::toggle-playback
 (fn [db _]
   (update-in db [::playback :play] not)))

(rf/reg-event-db
 ::time-updated
 (fn [db [_ e]]
   (js/console.log e)
   db))

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
                  (not play) (.pause video))
            ))]
    (r/create-class
     {:component-did-update update
      :component-did-mount (fn [comp]
                             (set! (.-src @!video) (:src (r/props comp)))
                             (update comp))
      :reagent-render
      (fn []
        [ui/paper {:style {:width "50%"
                           :max-width "480px"
                           :display "flex"
                           :margin "8px"
                           :padding "8px"}}
         [:video.project {:ref #(reset! !video %)
                          :on-click #(rf/dispatch [::toggle-playback])
                          :on-time-update #(rf/dispatch [::time-updated (-> % .-target .-currentTime)])}]])})))

(defn- media-panel-outer []
  (let [playback (rf/subscribe [::playback])]
    (fn []
      [media-panel-inner @playback])))

(defn- upper-panel []
  [:div.upper-panel
   [media-panel-outer]
   [upper-right-panel]])

(defn- lower-panel []
  [ui/paper {:style {:width "100%"
                     :margin "8px"
                     :padding "8px"}}])

(defn project-edit-panel-body []
  (r/with-let [doc (rf/subscribe [::current-project])]
    [:div
     [upper-panel]
     [lower-panel]]))
