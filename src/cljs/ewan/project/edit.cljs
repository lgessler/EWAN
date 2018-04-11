(ns ewan.project.edit
  (:require [re-frame.core :as rf]
            [ewan.spec.eaf30 :as eaf30]
            [cljsjs.material-ui]
            [cljs-react-material-ui.core]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as ic]
            [reagent.core :as r]))

;; subs
(rf/reg-sub
 ::current-project
 (fn [db _]
   (::current-project db)))

;; events
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
    :dispatch [:ewan.core/set-active-panel :project-edit-panel]}))

(rf/reg-event-db
 ::project-doc-fetched
 (fn [db [_ doc]]
   (assoc db ::current-project doc)))

(defn project-edit-panel-body [doc]
  (r/with-let [doc (rf/subscribe [::current-project])]
    [:div (str @doc)]))
