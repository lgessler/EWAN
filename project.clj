(defproject ewan "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.238"]
                 [org.clojure/data.xml "0.2.0-alpha5"]

                 ;; db
                 [cljsjs/pouchdb "6.3.4-0"]

                 ;; routing and state management
                 [secretary "1.2.3"]
                 [re-frame "0.10.5"]

                 ;; views
                 [reagent "0.8.0"]
                 [cljs-react-material-ui "0.2.48"]

                 ;; used for time formatting
                 [com.andrewmcveigh/cljs-time "0.5.0"]
                 ]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-less "1.7.5"]]

  :min-lein-version "2.5.3"

  ;; point this to src/cljs for the macro files that are in there (currently
  ;; there's only eaf30.clj).
  :source-paths ["src/cljs"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"
                                    "test/js"]

  :figwheel {:css-dirs ["resources/public/css"]
             :reload-clj-files {:clj true :cljc true}}

  :less {:source-paths ["less"]
         :target-path  "resources/public/css"}

  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

  :profiles
  {:dev
   {:dependencies [[binaryage/devtools "0.9.10"]
                   [day8.re-frame/re-frame-10x "0.3.3-react16"]
                   [figwheel-sidecar "0.5.15"]
                   [org.clojure/tools.nrepl "0.2.13"]
                   [com.cemerick/piggieback "0.2.1"]]

    :plugins      [[lein-figwheel "0.5.15"]
                   [lein-doo "0.1.8"]]}}

  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src/cljs"]
     :figwheel     {:on-jsload "ewan.core/mount-root"}
     :compiler     {:main                 ewan.core
                    :output-to            "resources/public/js/compiled/app.js"
                    :output-dir           "resources/public/js/compiled/out"
                    :asset-path           "js/compiled/out"
                    :source-map-timestamp true
                    :preloads             [devtools.preload
                                           day8.re-frame-10x.preload]
                    :closure-defines      {"re_frame.trace.trace_enabled_QMARK_" true}
                    :external-config      {:devtools/config {:features-to-install :all}}
                    :recompile-dependents false}}

    {:id           "min"
     :source-paths ["src/cljs"]
     :assert       false
     :compiler     {:main            ewan.core
                    :output-to       "resources/public/js/compiled/app.js"
                    :optimizations   :advanced
                    :closure-defines {goog.DEBUG false}
                    :pretty-print    false}}

    {:id           "test"
     :source-paths ["src/cljs" "test/cljs"]
     :compiler     {:main          ewan.runner
                    :output-to     "resources/public/js/compiled/test.js"
                    :output-dir    "resources/public/js/compiled/test/out"
                    :optimizations :none}}
    ]}

  )
