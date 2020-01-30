(defproject origami-fun "0.1.0-SNAPSHOT"
  :plugins [[hellonico/lein-gorilla "0.4.2"][lein-jupyter "0.1.16"]]
  :test-paths ["test" "samples"]
  :gorilla-options {:load-scan-exclude #{".git" "project.clj" ".svn" "samples" "src" "test"}}
  :repositories [["vendredi" "https://repository.hellonico.info/repository/hellonico/"]]
  :aliases {"notebook" ["gorilla" ":ip" "0.0.0.0" ":port" "10000"]
            "cv_ok" ["run" "-m" "opencv4.ok"]}
  :profiles {:dev {
    :dependencies [
    [hellonico/gorilla-repl "0.4.1"]
  ]}}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/tools.cli "0.3.5"]
                 [seesaw "1.4.5"]
                 [origami "4.2.0-0"]
                 [origami/filters "1.0"]])