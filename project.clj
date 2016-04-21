(defproject oak "0.1.3-SNAPSHOT"
  :description "Drop-dead simple, super-compositional UI components"
  :url "http://github.com/tel/cljs-oak"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.8.40"]
                 [quiescent/quiescent "0.3.1"]
                 [prismatic/schema "1.1.0"]]
  :plugins [[lein-figwheel "0.5.1"]
            [lein-cljsbuild "1.1.3"]
            [lein-doo "0.1.6"]]
  :clean-targets ^{:protect false} ["resources/public/js" "target"]
  :doo {:paths {:chrome "chrome --no-sandbox"
                :karma "node_modules/.bin/karma"}}
  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["deploy" "clojars"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]]

  :profiles
  {:dev {:dependencies [[devcards "0.2.1-6"]
                        [org.clojure/core.match "0.3.0-alpha4"]
                        [funcool/httpurr "0.5.0"]
                        [datascript "0.15.0"]
                        [funcool/promesa "1.1.1"]
                        [forest "0.1.4"]
                        [com.cognitect/transit-cljs "0.8.237"]]}}

  :cljsbuild
  {:builds [{:id "example"
             :source-paths ["src" "ex"]
             :figwheel {:devcards true}
             :compiler {:main oak.devcards
                        :asset-path "js/out"
                        :output-to "resources/public/js/example.js"
                        :output-dir "resources/public/js/out"
                        :source-map-timestamp true}}
            {:id "test"
             :source-paths ["src" "test"]
             :compiler {:output-to "resources/public/js/testable.js"
                        :output-dir "resources/public/js/out"
                        :main oak.test_runner
                        :optimizations :none}}]})