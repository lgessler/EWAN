(ns ewan.project.upload-form
  (:require [re-frame.core :as rf]
            [ewan.spec.eaf30 :as eaf30]
            [cljsjs.material-ui]
            [cljs-react-material-ui.core]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as ic]
            [reagent.core :as r]
            [cljs.pprint :as pprint]
            [ewan.spec.eaf30 :as eaf])
  (:require-macros [cljs.spec.alpha :as spec]))

;; TODO: some areas of this code use getElementById--they should use refs instead.
;; See: https://gist.github.com/pesterhazy/4d9df2edc303e5706d547aeabe0e17e1
;; I'm so sorry about the quality of this code. I probably won't fix it.

;; the form displayed in the dialog made in project.core
(def ^{:private true} default-form-state
  {:name ""
   :name-err ""
   :author ""
   :date (js/Date.)
   :file nil
   :eaf nil
   :files []
   :files-err ""})

(defn- name-error-text
  [name]
  (if (> (count name) 0)
    ""
    "Project must have a name."))

(defn- files-error-text
  [files]
  (if (some string? files)
    "You must upload all the media files referenced by this ELAN file."
    ""))

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
    (swap! state assoc :files-err (files-error-text (:files @state)))
    (cond
      (> (count (:name-err @state)) 0) (.. js/document
                                           (getElementById "upload-project-dialog-form-name-field")
                                           focus)
      (> (count (:files-err @state)) 0) nil
      :else (submit-callback @state))))

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

(defn- media-descriptor->filename
  [md]
  (-> md
      second
      :media-url
      (clojure.string/split #"/")
      last))

(defn- file-required?
  [filename state]
  (some #{filename} (map media-descriptor->filename
                         (eaf30/get-media-descriptors (:eaf @state)))))

(defn upload-project-dialog-form
  "Renders a form that captures the necessary information to create a new
  project. Takes a single argument, `submit-callback`, which is applied
  with the form's current state after it has been validated and submitted."
  [submit-callback]
  (r/with-let [state (r/atom default-form-state)]
    (fn []
      [:form#upload-project-dialog-form
       {:on-submit (attempt-submit state submit-callback)}
       [:label {:for "upload-project-dialog-form-file-field"} "ELAN file"]
       [:input#upload-project-dialog-form-file-field
        {:type "file"
         :accept ".eaf,.xml"
         :style {:display "none"}
         :on-change (fn [e]
                      (let [file (-> e .-target .-files (aget 0))
                            name (.-name file)
                            prjname (subs name 0 (- (count name) 4))
                            reader (js/FileReader.)]
                        (set! (.-onload reader)
                              (fn [e]
                                (let [hiccup (eaf30/eaf-str->hiccup
                                              (-> e .-target .-result))]
                                  (swap! state assoc :eaf hiccup)
                                  (swap! state assoc :author (eaf30/get-author hiccup))
                                  (swap! state assoc :date (js/Date. (eaf30/get-date hiccup)))
                                  (swap! state update :files into
                                         (map media-descriptor->filename (eaf30/get-media-descriptors hiccup))))))
                        (.readAsText reader file)
                        (swap! state assoc :file file)
                        (swap! state assoc :name prjname)))}]
       (if (some? (:file @state))
         [:div.upload-project-dialog-form__file-selection--filled
          (-> @state :file (aget "name"))]
         [:div.upload-project-dialog-form__file-selection
          [ui/raised-button
           {:primary true
            :label "Upload file"
            :label-position "after"
            :icon (ic/file-file-upload)
            :disable-touch-ripple true
            :on-click #(-> (.getElementById
                            js/document
                            "upload-project-dialog-form-file-field")
                           (.click))}]])

       [:label {:for "upload-project-dialog-form-name-field"} "Project name"]
       [ui/text-field
        {:id "upload-project-dialog-form-name-field"
         :auto-focus "autofocus"
         :full-width true
         :floating-label-fixed true
         :error-text (:name-err @state)
         :value (:name @state)
         :on-change (fn [_ v]
                      (swap! state assoc :name v)
                      (swap! state assoc :name-err (name-error-text v)))}]
       [:label {:for "upload-project-dialog-form-author-field"} "Author"]
       [ui/text-field
        {:id "upload-project-dialog-form-author-field"
         :full-width true
         :floating-label-fixed true
         :on-change #(swap! state assoc :author %2)}]
       [:label {:for "upload-project-dialog-form-date-picker"} "Date"]
       [ui/date-picker
        {:id "upload-project-dialog-form-date-picker"
         :hint-text "Date"
         :value (:date @state)
         :on-change #(swap! state assoc :date %2)}]


       [:label {:for "upload-project-dialog-form-file-upload"} "Media files"]
       [ui/table
        {:on-cell-click (fn [row-id]
                          (let [file (nth (:files @state) row-id)]
                            (cond (string? file)
                                  (.click (js/document.getElementById "upload-project-dialog-form-file-upload"))
                                  (not (file-required? (.-name file) state))
                                  (swap! state update :files
                                         #(vec (concat
                                                (subvec % 0 row-id)
                                                (subvec % (inc row-id))))))))
         :wrapper-style {:max-height "200px"}
         :selectable false}
        [ui/table-body
         {:display-row-checkbox false
          :show-row-hover true}
         (for [file (:files @state)]
           [ui/table-row {:key (if (string? file)
                                 file
                                 (-> file unique-file-map vals str))
                          :style (if (or (string? file)
                                         (not (file-required? (.-name file) state)))
                                   {:cursor "pointer"}
                                   {})}
            [ui/table-row-column
             [ui/icon-button
              {:style {:vertical-align "middle"}}
              (cond (string? file) [ic/file-file-upload {:color "red"}]
                    (not (file-required? (.-name file) state)) [ic/navigation-close]
                    :else [ui/font-icon {:class-name "material-icons"} "done"])]

             [:div {:style {:display "inline-block"
                            :vertical-align "middle"}}
              (or (.-name file) file)]]

            [ui/table-row-column
             (if (string? file)
               ""
               (fmt-size (.-size file)))]])]]
       [:div.upload-project-dialog-form__file-error-text (:files-err @state)]

       [ui/flat-button {:label "Add files"
                        :primary false
                        :on-click
                        #(.. js/document
                             (getElementById "upload-project-dialog-form-file-upload")
                             click)}]
       [:input
        {:id "upload-project-dialog-form-file-upload"
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
                      (->> cur-files
                           (filter (fn [file]
                                     (not
                                      (and (string? file)
                                           (some #{file} (map #(.-name %) new-files))))))
                           (into new-files)
                           vec)))))}]])))


