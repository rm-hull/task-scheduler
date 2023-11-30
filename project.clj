(defproject rm-hull/task-scheduler "0.2.1"
  :description "Fork/Join task scheduling in Clojure"
  :url "https://github.com/rm-hull/task-scheduler"
  :license {
    :name "The MIT License (MIT)"
    :url "http://opensource.org/licenses/MIT"}
  :dependencies [ ]
  :scm {:url "git@github.com:rm-hull/task-scheduler.git"}
  :vcs :git
  :source-paths ["src"]
  :jar-exclusions [#"(?:^|/).git"]
  :codox {
    :source-paths ["src"]
    :output-path "doc/api"
    :source-uri "http://github.com/rm-hull/task-scheduler/blob/main/{filepath}#L{line}"}
  :min-lein-version "2.8.1"
  :profiles {
    :dev {
      :global-vars {*warn-on-reflection* true}
      :plugins [
        [lein-codox "0.10.8"]
        [lein-cljfmt "0.9.2"]
        [lein-cloverage "1.2.4"]]
      :dependencies [
        [org.clojure/clojure "1.11.1"]]}})

