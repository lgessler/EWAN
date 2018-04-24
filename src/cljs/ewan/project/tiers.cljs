(ns ewan.project.edit.tiers
  (:require [re-frame.core :as rf]
            [ewan.spec.eaf30 :as eaf30]
            [cljsjs.material-ui]
            [cljs-react-material-ui.core]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as ic]
            [reagent.core :as r]))

(def ^:private default-db {::px-per-sec 50})


;; ----------------------------------------------------------------------------
;; subs
;; ----------------------------------------------------------------------------
(rf/reg-sub ::px-per-sec (fn [db] (::px-per-sec db)))
(rf/reg-sub
 ::tiers
 (fn [db]
   (-> db
       :ewan.project.edit/current-project
       :eaf
       eaf30/get-tiers)))

(defn- tier-rows [tiers]
  [:div.tier-rows__container
   (for [tier @tiers]
     [:div.tier-rows__row "Hello, world! the rest is the best  the rest is the best  the rest is the best  the rest is the best  the rest is the best  the rest is the best  the rest is the best  the rest is the best  the rest is the best  the rest is the best  the rest is the best  the rest is the best  the rest is the best  the rest is the best  the rest is the best  the rest is the best  the rest is the best  the rest is the best  the rest is the best  the rest is the best "])])

(defn- tier-labels [tiers]
  [:div.tier-labels__container
   (for [tier @tiers]
     [:div.tier-labels__row
      (:tier-id (second tier))])])

  (defn tiers []
    (let [tiers (rf/subscribe [::tiers])
          rows (atom nil)]
      (fn []
        [ui/paper
         {:style {:margin "6px"}}
         [:div {:style {:width "100%"
                        :overflow-x "auto"}}
         [tier-labels tiers]
         [tier-rows tiers]]])))

