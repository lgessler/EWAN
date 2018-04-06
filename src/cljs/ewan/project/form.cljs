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

;; the form displayed in the dialog made in project.core for creating a new
;; project. Note that the form does not actually contain the Create and
;; Cancel buttons--rather these are passed into the dialog's `actions` prop,
;; and the Create button points to the form using the form's ID. Not sure
;; if this was the cleanest solution--there is at least one more alternative:
;; https://stackoverflow.com/questions/40881616/how-to-submit-the-form-by-material-ui-dailog-using-reactjs

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
    (js/console.log @state)
    (if (> (count (:name @state)) 0)
      (submit-callback)
      (.. js/document
          (getElementById "new-project-dialog-form-name-field")
          focus))))

(defn new-project-dialog-form
  "Renders a form that captures the necessary information to create a new
  project. Takes a single argument, `submit-callback`, which is executed
  after the form has been validated and submitted. This callback should
  probably dispatch a re-frame event."
  [submit-callback]
  (let [state (r/atom default-form-state)
        update-name (fn [e]
                      (let [s (.. e -target -value)]
                        (swap! state assoc :name s)
                        (swap! state assoc :name-err (name-error-text s))))]
    (fn []
      [:form#new-project-dialog-form {:on-submit (attempt-submit state submit-callback)}
       [ui/text-field {:id "new-project-dialog-form-name-field"
                       :floating-label-text "Project name"
                       :full-width true
                       :auto-focus "autofocus"
                       :error-text (:name-err @state)
                       :on-key-press #(when (= (.-keyCode %) 13)
                                        (attempt-submit state submit-callback))
                       :on-blur update-name}]
       [ui/text-field {:floating-label-text "Author"
                       :full-width true
                       :on-blur #(swap! state assoc :author (-> % .-target .-value))}]
       [ui/date-picker {:hint-text "Date"
                        :value (:date @state)
                        :on-change #(swap! state assoc :date %2)}]

       ])))




