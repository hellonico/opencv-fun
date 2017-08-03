(defproject egami "0.1.0-SNAPSHOT"
  :injections [
  (clojure.lang.RT/loadLibrary org.opencv.core.Core/NATIVE_LIBRARY_NAME)
  ]
  :plugins [
  [lein-gorilla "0.4.0"]]
  :test-paths ["test" "samples"]
  :repositories [["vendredi" "http://hellonico.info:8081/repository/hellonico/"]]
  :profiles {:dev {
    :repl-options {:init-ns opencv3.affine}
    :dependencies [
    [org.clojure/tools.nrepl "0.2.11"]
    [proto-repl "0.3.1"]
    [camel-snake-kebab "0.4.0"]
    [gorilla-repl "0.4.0"] ]}}
  :dependencies [
   [org.clojure/clojure "1.8.0"]
   [org.clojure/tools.cli "0.3.5"]

   [opencv/opencv "3.3.0-rc"]
  ;  [opencv/opencv-native "3.3.0-rc-osx"]
   [opencv/opencv-native "3.3.0-rc"]
   [opencv/opencv-native "3.3.0-rc-linux"]
   [opencv/opencv-native "3.3.0-rc-win"]


  ])
