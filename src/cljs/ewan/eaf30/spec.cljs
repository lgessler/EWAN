(ns ewan.eaf30.spec
  (:require [cljs.spec.alpha :as s]
            [cljs-time.format :as timefmt])
  (:require-macros [cljs.spec.alpha :as s]))

;; This is as near a translation as possible of the EAF 3.0 format as described
;; by its schema:
;;    - http://www.mpi.nl/tools/elan/EAFv3.0.xsd
;;    - http://www.mpi.nl/tools/elan/EAF_Annotation_Format_3.0_and_ELAN.pdf
;; A number will be given before each element which corresponds to the section
;; in the PDF guide that describes it.
;;
;; One major difference is that the ANNOTATION_TAG element does NOT have the
;; `xmlns:xsi` and `xsi:noNamespaceSchemaLocation` tags because the CLJS
;; implementation of `clojure.data.xml` was being too helpful. Instead,
;; we will just make sure to strip these attrs out when we encounter them
;; and make sure to write them back when we write back to an XML string.
;;
;; Note that we've had to give the definitions in a different order because of
;; how spec expects all specs used in a dependent spec to be defined before
;; reference.
;;

;; 2.2 license
;; --------------------------------------------
(s/def ::license-url string?)
(s/def ::license (s/cat :tag #(= % :license)
                        :attrs (s/keys :opt-un [::license-url])
                        :contents string?))

;; 2.3 header
;; --------------------------------------------
;; 2.3.1 media descriptor
(s/def ::media-url string?)
(s/def ::mime-type string?)
(s/def ::relative-media-url string?)
(s/def ::time-origin string?) ;; this should be a number, see Note 1 at top
(s/def ::extracted-from string?)
(s/def ::media-descriptor
  (s/cat :tag #(= % :media-descriptor)
         :attrs (s/keys :req-un [::media-url
                                 ::mime-type]
                        :opt-un [::relative-media-url
                                 ::time-origin
                                 ::extracted-from])))

;; 2.3.2 linked file descriptor
;; mime-type and time-origin defined in 2.3.1
(s/def ::link-url string?)
(s/def ::relative-link-url string?)
(s/def ::associated-with string?)
(s/def ::linked-file-descriptor
  (s/cat :tag #(= % :linked-file-descriptor)
         :attrs (s/keys :req-un [::link-url
                                 ::mime-type]
                        :opt-un [::relative-link-url
                                 ::time-origin
                                 ::associated-with])))

;; 2.3.3 properties
(s/def ::name string?)
(s/def ::property
  (s/cat :tag #(= % :property)
         :attrs (s/keys :opt-un [::name])
         :content string?))

(s/def ::media-file string?)
(s/def ::time-units #{"milliseconds" "PAL-frames" "NTSC-frames"})
(s/def ::header
  (s/cat :tag #(= % :header)
         :attrs (s/keys :opt-un [::media-file ::time-units])
         :media-descriptors (s/* (s/spec ::media-descriptor))
         :linked-file-descriptors (s/* (s/spec ::linked-file-descriptor))
         :properties (s/* (s/spec ::property))))


;; 2.4 time order
;; --------------------------------------------
;; 2.4.1 time slots
(s/def ::time-slot-id string?)
;; this should actually ensure that TIME_VALUE holds a non-negative
;; integer. See Note 1 at top
(s/def ::time-value string?)
(s/def ::time-slot
  (s/cat :tag #(= % :time-slot)
         :attrs (s/keys :req-un [::time-slot-id] :opt-un [::time-value])))

(s/def ::time-order
  (s/cat :tag #(= % :time-order)
         :attrs map?
         :time-slots (s/* (s/spec ::time-slot))))



;; 2.5 tier
;; --------------------------------------------
;; 2.5.2 alignable annotation
(s/def ::annotation-id string?) ;; next 4 from 2.5.5--also used in 2.5.3
(s/def ::ext-ref string?)
(s/def ::lang-ref string?)
(s/def ::cve-ref string?)
(s/def ::time-slot-ref1 string?)
(s/def ::time-slot-ref2 string?)
(s/def ::svg-ref string?)

;; from 2.5.4--also used in 2.5.3
(s/def ::annotation-value
  (s/cat :tag #(= % :annotation-value)
         :attrs map?
         :contents (s/? string?)))

(s/def ::alignable-annotation
  (s/cat :tag #(= % :alignable-annotation)
         :attrs (s/keys :req-un [::annotation-id
                                 ::time-slot-ref1
                                 ::time-slot-ref2]
                        :opt-un [::svg-ref
                                 ::ext-ref
                                 ::lang-ref
                                 ::cve-ref])
         :annotation-value (s/spec ::annotation-value)))

;; 2.5.3 ref annotation
(s/def ::annotation-ref string?)
(s/def ::previous-annotation string?)
(s/def ::ref-annotation
  (s/cat :tag #(= % :ref-annotation)
         :attrs (s/keys :req-un [::annotation-id
                                 ::annotation-ref]
                        :opt-un [::previous-annotation
                                 ::ext-ref
                                 ::lang-ref
                                 ::cve-ref])
         :annotation-value (s/spec ::annotation-value)))

;; 2.5.1 annotation
(s/def ::annotation
  (s/cat :tag #(= % :annotation)
         :attrs map?
         :child (s/alt :alignable-annotation
                       (s/spec ::alignable-annotation)
                       :ref-annotation
                       (s/spec ::ref-annotation))))

(s/def ::tier-id string?)
(s/def ::participant string?)
(s/def ::annotator string?)
(s/def ::linguistic-type-ref string?)
(s/def ::default-locale string?)
(s/def ::parent-ref string?)
(s/def ::ext-ref string?)
(s/def ::tier
  (s/cat :tag #(= % :tier)
         :attrs (s/keys :req-un [::tier-id
                                 ::linguistic-type-ref]
                        :opt-un [::participant
                                 ::annotator
                                 ::default-locale
                                 ::parent-ref
                                 ::ext-ref
                                 ::lang-ref]) ;; already defined in 2.5.2
         :annotations (s/* (s/spec ::annotation))))

;; 2.6 linguistic type
;; --------------------------------------------
(s/def ::linguistic-type-id string?)
(s/def ::time-alignable #{"true" "false"})
(s/def ::constraints string?)
(s/def ::graphic-references #{"true" "false"})
(s/def ::controlled-vocabulary-ref string?)
(s/def ::lexicon-ref string?)
(s/def ::linguistic-type
  (s/cat :tag #(= % :linguistic-type)
         :attrs (s/keys :req-un [::linguistic-type-id]
                        :opt-un [::time-alignable
                                 ::constraints
                                 ::graphic-references
                                 ::controlled-vocabulary-ref
                                 ::ext-ref
                                 ::lexicon-ref])))
;; ext-ref is defined above in 2.5.1. There is a slight semantic difference
;; here since 2.5.1's ext ref allows multiple refs, but since we're only
;; checking if it's a string in this spec, it doesn't matter.

;; 2.7 constraint
;; --------------------------------------------
(s/def ::stereotype #{"Time_Subdivision" "Symbolic_Subdivision"
                      "Symbolic_Association" "Included_In"})
(s/def ::description string?)
(s/def ::constraint
  (s/cat :tag #(= % :constraint)
         :attrs (s/keys :req-un [::stereotype]
                        :opt-un [::description])))


;; 2.9 controlled vocabulary
;; --------------------------------------------

;; 2.9.2 cve value
(s/def ::cve-value
  (s/cat :tag #(= % :cve-value)
         :attrs (s/keys :req-un [::lang-ref] ;; already defined in 2.5.2
                        :opt-un [::description]) ;; 2.7
         :contents string?))

;; 2.9.1 cv entry ml
(s/def ::cve-id string?)
(s/def ::cv-entry-ml
  (s/cat :tag #(= % :cv-entry-ml)
         :attrs (s/keys :req-un [::cve-id]
                        :opt-un [::ext-ref]) ;; defined in 2.5.1
         :values (s/+ (s/spec ::cve-value))))

;; 2.9.3 description
;; tag is actually called DESCRIPTION, but there is a collision with 2.7
(s/def ::cv-description
  (s/cat :tag #(= % :description)
         :attrs (s/keys :req-un [::lang-ref]) ;; already defined in 2.5.2
         :contents string?))

(s/def ::cv-id string?)
(s/def ::controlled-vocabulary
  (s/cat :tag  #(= % :controlled-vocabulary)
         :attrs (s/keys :req-un [::cv-id]
                        :opt-un [::ext-ref]) ;; defined in 2.5.1
         :descriptions (s/* (s/spec ::cv-description))
         :cv-entry-ml (s/* (s/spec ::cv-entry-ml))
         ))

;; 2.10 external ref
;; --------------------------------------------
(s/def ::ext-ref-id string?)
(s/def ::type #{"iso12620" "ecv" "cve_id" "lexen_id" "resource_url"})
(s/def ::value string?)
(s/def ::external-ref
  (s/cat :tag #(= % :external-ref)
         :attrs (s/keys ::req-un [::ext-ref-id
                                  ::type
                                  ::value])))

;; 2.11 locale
;; --------------------------------------------
(s/def ::language-code string?)
(s/def ::country-code string?)
(s/def ::variant string?)
(s/def ::locale
  (s/cat :tag #(= % :locale)
         :attrs (s/keys ::req-un [::language-code]
                        ::opt-un [::country-code
                                  ::variant])))

;; 2.12 language
;; --------------------------------------------
(s/def ::lang-id string?)
(s/def ::lang-def string?)
(s/def ::lang-label string?)
(s/def ::language
  (s/cat :tag #(= % :language)
         :attrs (s/keys ::req-un [::lang-id]
                        ::opt-un [::lang-def
                                  ::lang-label])))

;; 2.13 lexicon ref
;; --------------------------------------------
;; some of these have previous collisions, so just namespace them all
(s/def :ewan.eaf30.lexicon-ref/lex-ref-id string?)
(s/def :ewan.eaf30.lexicon-ref/name string?)
(s/def :ewan.eaf30.lexicon-ref/type string?)
(s/def :ewan.eaf30.lexicon-ref/url string?)
(s/def :ewan.eaf30.lexicon-ref/lexicon-id string?)
(s/def :ewan.eaf30.lexicon-ref/lexicon-name string?)
(s/def :ewan.eaf30.lexicon-ref/datcat-id string?)
(s/def :ewan.eaf30.lexicon-ref/datcat-name string?)
(s/def ::lexicon-ref
  (s/cat :tag #(= % :lexicon-ref)
         :attrs (s/keys ::req-un [:ewan.eaf30.lexicon-ref/lex-ref-id
                                  :ewan.eaf30.lexicon-ref/name
                                  :ewan.eaf30.lexicon-ref/type
                                  :ewan.eaf30.lexicon-ref/url
                                  :ewan.eaf30.lexicon-ref/lexicon-id
                                  :ewan.eaf30.lexicon-ref/lexicon-name]
                        ::opt-un [:ewan.eaf30.lexicon-ref/datcat-id
                                  :ewan.eaf30.lexicon-ref/datcat-name])))

;; 2.14 ref link set
;; --------------------------------------------
;; 2.14.3 refLinkAttribute
(s/def ::ref-link-id string?)
(s/def ::ref-link-name string?)
;; ext-ref, lang-ref, cve-ref already defined in 2.5
(s/def ::ref-type string?)

;; 2.14.1 cross ref link
(s/def ::ref1 string?)
(s/def ::ref2 string?)
(s/def ::directionality #{"undirected" "unidirectional" "bidirectional"})
(s/def ::cross-ref-link
  (s/cat :tag #(= % :cross-ref-link)
         :attrs (s/keys ::req-un [::ref1
                                  ::ref2
                                  ::ref-link-id]
                        ::opt-un [::directionality
                                  ::ref-link-name
                                  ::ext-ref
                                  ::lang-ref
                                  ::cve-ref
                                  ::ref-type])))

;; 2.14.2 group ref link
(s/def ::refs string?)
(s/def ::group-ref-link
  (s/cat :tag #(= % :group-ref-link)
         :attrs (s/keys ::req-un [::refs
                                  ::ref-link-id]
                        ::opt-un [::ref-link-name
                                  ::ext-ref
                                  ::lang-ref
                                  ::cve-ref
                                  ::ref-type])))

(s/def ::link-set-id string?)
(s/def ::link-set-name string?)
(s/def ::cv-ref string?)
(s/def ::ref-link-set
  (s/cat :tag #(= % :ref-link-set)
         :attrs (s/keys ::req-un [::link-set-id]
                        ::opt-un [::link-set-name
                                  ::ext-ref       ;; from 2.5
                                  ::lang-ref      ;; from 2.5
                                  ::cv-ref])
         :links (s/* (s/alt :cross-ref-link
                            (s/spec ::cross-ref-link)
                            :group-ref-link
                            (s/spec ::group-ref-link)))))


;; 2.1 annotation document
;; --------------------------------------------
(s/def ::author string?)
(s/def ::date #(some? (timefmt/parse %)))
(s/def ::version string?)
(s/def ::format string?)
;; Note that the order of the seq in the XSD is NOT the same as that of
;; the section numbers in the explanatory document
(s/def ::annotation-document
  (s/cat
   :tag   #(= % :annotation-document)
   :attrs (s/and
           (s/keys :req-un [::author ::date ::version]
                   :opt-un [::format])
           ;; if present, format must match version
           #(if (:format %)
              (= (:format %) (:version %))
              true))
   :license (s/* (s/spec ::license)) ;; 2.2
   :header (s/spec ::header) ;; 2.3
   :time-order (s/spec ::time-order) ;; 2.4
   :tiers (s/* (s/spec ::tier)) ;; 2.5
   :linguistic-type (s/* (s/spec ::linguistic-type)) ;; 2.6
   :locale (s/* (s/spec ::locale)) ;; 2.11
   :language (s/* (s/spec ::language)) ;; 2.12
   :constraints (s/* (s/spec ::constraint)) ;; 2.7
   :controlled-vocabulary (s/* (s/spec ::controlled-vocabulary)) ;; 2.9
   :lexicon-ref (s/* (s/spec ::lexicon-ref)) ;; 2.13
   :ref-link-set (s/* (s/spec ::ref-link-set)) ;; 2.14
   :external-ref (s/* (s/spec ::external-ref)) ;; 2.10
   ))

