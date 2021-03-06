(ns ewan.project.new-form
  (:require [re-frame.core :as rf]
            [ewan.eaf30.core :as eaf30]
            [cljsjs.material-ui]
            [cljs-react-material-ui.core]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as ic]
            [reagent.core :as r]
            [cljs.pprint :as pprint]
            )
  (:require-macros [cljs.spec.alpha :as spec]))

;; TODO: some areas of this code use getElementById--they should use refs instead.
;; See: https://gist.github.com/pesterhazy/4d9df2edc303e5706d547aeabe0e17e1

;; the form displayed in the dialog made in project.core
(def ^{:private true} default-form-state
  {:name ""
   :name-err ""
   :author ""
   :date (js/Date.)
   :files []})

(defn- name-error-text
  [name]
  (if (> (count name) 0)
    ""
    "Project must have a name."))

(defn- unique-file-map
  "Not guaranteed to be unique, but will be good enough 99% of the time"
  [f]
  {:name (.-name f)
   :last-mod (.-lastModified f)
   :size (.-size f)})

(defn- attempt-submit
  [state submit-callback]
  (fn [e]
    (.preventDefault e)
    (swap! state assoc :name-err (name-error-text (:name @state)))
    (if (> (count (:name-err @state)) 0)
      (.. js/document
          (getElementById "new-project-dialog-form-name-field")
          focus)
      (submit-callback @state))))

(defn- one-decimal-trim
  [f]
  (pprint/cl-format nil "~,1f" f))

(defn- fmt-size
  [size]
  (condp <= size
    1000000000 (-> size
                   (/ 1000000000)
                   one-decimal-trim
                   (str " GB"))
    1000000 (-> size
                (/ 1000000)
                one-decimal-trim
                (str " MB"))
    1000 (-> size
             (/ 1000)
             one-decimal-trim
             (str " KB"))
    (str size " B")))

(defn new-project-dialog-form
  "Renders a form that captures the necessary information to create a new
  project. Takes a single argument, `submit-callback`, which is applied
  with the form's current state after it has been validated and submitted."
  [submit-callback]
  (r/with-let [state (r/atom default-form-state)]
    (fn []
      [:form#new-project-dialog-form
       {:on-submit (attempt-submit state submit-callback)}
       [:label {:for "new-project-dialog-form-name-field"} "Project name"]
       [ui/text-field
        {:id "new-project-dialog-form-name-field"
         :auto-focus "autofocus"
         :full-width true
         :floating-label-fixed true
         :error-text (:name-err @state)
         :on-change (fn [_ v]
                      (swap! state assoc :name v)
                      (swap! state assoc :name-err (name-error-text v)))}]
       [:label {:for "new-project-dialog-form-author-field"} "Author"]
       [ui/text-field
        {:id "new-project-dialog-form-author-field"
         :full-width true
         :floating-label-fixed true
         :on-change #(swap! state assoc :author %2)}]
       [:label {:for "new-project-dialog-form-date-picker"} "Date"]
       [ui/date-picker
        {:id "new-project-dialog-form-date-picker"
         :hint-text "Date"
         :value (:date @state)
         :on-change #(swap! state assoc :date %2)}]

       [:label {:for "new-project-dialog-form-file-upload"} "Media files"]
       [ui/table
        {:on-cell-click (fn [row-id]
                          (swap! state update :files
                                 #(vec (concat
                                        (subvec % 0 row-id)
                                        (subvec % (inc row-id))))))
         :wrapper-style {:max-height "200px"}
         :selectable false}
        [ui/table-body
         {:display-row-checkbox false
          :show-row-hover true}
         (doall
          (for [file (:files @state)]
            [ui/table-row {:key (-> file unique-file-map vals str)}
             [ui/table-row-column
              [:svg {:view-box "0 0 20 20"
                     :width "20"
                     :height "20"
                     :style {:display "inline-block"
                             :vertical-align "middle"
                             :margin-right "12px"
                             :cursor "pointer"}}
               [ic/navigation-close]]
              [:div {:style {:display "inline-block"
                             :vertical-align "middle"}}
               (.-name file)]]
             [ui/table-row-column (fmt-size (.-size file))]]))]]
       [ui/raised-button {:label "Add files"
                          :primary true
                          :on-click
                          #(.. js/document
                               (getElementById "new-project-dialog-form-file-upload")
                               click)}]
       [:input
        {:id "new-project-dialog-form-file-upload"
         :type "file"
         :multiple "multiple"
         :style {:display "none"}
         :on-change
         (fn [e]
           (swap! state update :files
                  (fn [cur-files]
                    (let [already-present (into #{} (map unique-file-map cur-files))
                          new-files (filter #(not (contains? already-present (unique-file-map %)))
                                            (-> e .-target .-files array-seq))]
                      (into cur-files new-files)))))}]])))


