(ns ewan.project.form
  (:require [re-frame.core :as rf]
            [ewan.spec.eaf30 :as eaf30]
            [cljsjs.material-ui]
            [cljs-react-material-ui.core]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as ic]
            [reagent.core :as r]
            [goog.functions])
  (:require-macros [cljs.spec.alpha :as spec]))

;; the form displayed in the dialog made in project.core for 

(def ^{:private true} default-form-state
  {:name ""
   :name-err ""
   :author ""
   :date (js/Date.)
   :media-descriptors (list)})

(defn- name-error-text
  [name]
  (if (> (count name) 0)
    ""
    "Project must have a name."))

(defn- attempt-submit
  [state submit-callback]
  (fn [e]
    (.preventDefault e)
    (swap! state assoc :name-err (name-error-text (:name @state)))
    (if (> (count (:name-err @state)) 0)
      (.. js/document
          (getElementById "new-project-dialog-form-name-field")
          focus)
      (submit-callback))))

(defn new-project-dialog-form
  "Renders a form that captures the necessary information to create a new
  project. Takes a single argument, `submit-callback`, which is executed
  after the form has been validated and submitted. This callback should
  probably dispatch a re-frame event."
  [submit-callback]
  (let [state (r/atom default-form-state)]
    (fn []
      [:form#new-project-dialog-form
       {:on-submit (attempt-submit state submit-callback)}
       [ui/text-field {:id "new-project-dialog-form-name-field"
                       :floating-label-text "Project name"
                       :full-width true
                       :floating-label-fixed true
                       :error-text (:name-err @state)
                       :on-change (fn [_ v]
                                    (swap! state assoc :name v)
                                    (swap! state assoc :name-err (name-error-text v)))}]
       [ui/text-field {:floating-label-text "Author"
                       :full-width true
                       :floating-label-fixed true
                       :on-change #(swap! state assoc :author %2)}]
       [ui/date-picker {:style {:margin-top "18px"}
                        :hint-text "Date"
                        :value (:date @state)
                        :on-change #(swap! state assoc :date %2)}]
       ])))


