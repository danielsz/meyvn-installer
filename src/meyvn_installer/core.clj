(ns meyvn-installer.core
  (:gen-class)
  (:require [clojure.string :as str :refer [trim-newline]]
            [meyvn-installer.utils :as utils :refer [exit find-file]]
            [clojure.java.io :as io]
            [clojure.tools.cli :refer [parse-opts]])
  (:import [java.nio.file Paths LinkOption]
           [java.io FileNotFoundException]))

(def version "1.6.1")
(def release (str (System/getProperty "user.home") "/.m2/repository/org/meyvn/meyvn/" version "/meyvn-" version ".jar"))

(defn maven-path []
  (let [pb (ProcessBuilder. ["which" "mvn"])
        process (.start pb)
        rc (.waitFor process)]
    (if (= rc 0)
      (let [path (-> (.getInputStream process)
                    slurp
                    trim-newline
                    (Paths/get (into-array String [])))]
        (.toRealPath path (into-array LinkOption [])))
      (exit "Maven executable not found. Please install Maven prior to Meyvn." :status 1))))

(defn maven-home []
  (-> (maven-path) .getParent .getParent))

(defn bin-path []
  (let [path (-> (System/getenv "PATH")
                (str/split #":"))
        homedir (System/getProperty "user.home")
        candidates #{(str homedir "/.local/bin") (str homedir "/bin") "/usr/local/bin"}
        exists #(.isDirectory (io/file %))]
    (first (filter (every-pred candidates exists) path))))

(defn download []
  (let [pb (ProcessBuilder. ["mvn" "org.apache.maven.plugins:maven-dependency-plugin:3.2.0:get" (str "-Dartifact=org.meyvn:meyvn:" version)])
        rc (.waitFor (-> pb .inheritIO .start))]
    (if (zero? rc)
      (println "Finished downloading")
      (exit "There was a problem downloading meyvn." :status 1))))

(def cli-options
 [["-u" "--username USERNAME" "The username that came with your license."]
  ["-p" "--password PASSWORD" "The password that came wiht your license."]
  ["-v" nil "Verbosity level, use as a flag (no arguments)" :id :verbose :default 0 :update-fn inc]
  ["-h" "--help" "This help screen."]])


(defn usage [summary]
  (->> ["This is the Meyvn installer."
        ""
        "Usage: clj -m meyvn-installer.core"
        ""
        summary
        ""
        ""]
       (str/join "\n")))

(defn -main [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options :in-order true)
        home (maven-home)
        sh (io/file (str (bin-path) "/myvn"))]
    (when (:help options) (exit (usage summary)))
    (when (pos? (:verbose options)) (println "options: " options "\narguments: " arguments "\nerrors: " errors))
    (if (find-file release)
      (println "Existing Meyvn jar found.")
      (download))
    (try
      (spit sh (str "M2_HOME=" home " java -jar " release " $@"))
      (.setExecutable sh true)
      (catch FileNotFoundException e
        (println (.getMessage e))
        (exit "Please consider having ~/bin or ~/.local/bin in your path rather than sudo'ing." :status 1)))
    (println (str "Meyvn has been successfully installed (" (str sh) ")." ))))
