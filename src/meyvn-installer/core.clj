(ns meyvn-installer.core
  (:gen-class)
  (:require [clojure.string :refer [trim-newline]])
  (:import [java.nio.file Path LinkOption]))


;; add username and password to settings.xml
;; Check if maven is installed

(defn -main [& args]
  (println "Hello, World daniel"))

(defn exit [msg & {:keys [status] :or {status 0}}]
  (println msg)
  (System/exit status))

(defn maven-path []
  (let [pb (ProcessBuilder. ["which" "mvn"])
        process (.start pb)
        rc (.waitFor process)]
    (if (= rc 0)
      (let [path (-> (.getInputStream process)
                    slurp
                    trim-newline
                    (Path/of (into-array String [])))]
        (.toRealPath path (into-array LinkOption [])))
      (exit "Maven executable not found" :status 1))))

(defn maven-home []
  (let [path (maven-path)
        length (.getNameCount path)]
    (.subpath path 0 (- length 2))))
