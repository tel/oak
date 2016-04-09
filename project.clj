(defproject irony "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.8.40"]
                 [quiescent/quiescent "0.3.0"]
                 [datascript "0.15.0"]
                 [mount "0.1.10"]
                 [prismatic/schema "1.1.0"]
                 [cljsjs/react-bootstrap "0.28.1-1" :exclusions [[cljsjs/react]]]
                 [org.clojure/core.match "0.3.0-alpha4"]]
  :plugins [[lein-figwheel "0.5.1"]
            [lein-cljsbuild "1.1.3"]]
  :clean-targets ^{:protect false} ["resources/public/js" "target"]
  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src/"]
                        :figwheel {:on-jsload "irony.core/reload"}
                        :compiler {:main "irony.core"
                                   :asset-path "js/out"
                                   :output-to "resources/public/js/app-dev.js"
                                   :output-dir "resources/public/js/out"}}
                       {:id "min"
                        :source-paths ["src/"]
                        :compiler {:output-to "resources/public/js/app.js"
                                   :main "irony.core"
                                   :optimizations :advanced
                                   :pretty-print false}}]})
