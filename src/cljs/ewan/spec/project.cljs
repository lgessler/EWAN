(ns ewan.spec.project
  (:require [cljs.spec.alpha :as spec]
            [ewan.spec.eaf30 :as eaf30])
  (:require-macros [cljs.spec.alpha :as spec]))

;; spec for the "project", i.e. a row in PouchDB
(spec/def ::name string?)
(spec/def ::eaf eaf30/eaf?) ;; a nearly-perfect hiccup repr of ELAN XML files
(spec/def ::project
  (spec/keys :req-un [::eaf ::name]))
