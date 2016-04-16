(defproject oak "0.1.0-SNAPSHOT"
  :description "Drop-dead simple, super-compositional UI components"
  :url "http://github.com/tel/cljs-oak"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.8.40"]
                 [quiescent/quiescent "0.3.0"]
                 [prismatic/schema "1.1.0"]]
  :plugins [[lein-figwheel "0.5.1"]
            [lein-cljsbuild "1.1.3"]
            [lein-doo "0.1.6"]]
  :clean-targets ^{:protect false} ["resources/public/js" "target"])
