(ns meyvn-installer.core
  (:gen-class)
  (:require [clojure.string :as str :refer [trim-newline]]
            [meyvn-installer.utils :as utils :refer [exit find-file]]
            [clojure.java.io :as io]
            [clojure.tools.cli :refer [parse-opts]])
  (:import [java.nio.file Paths LinkOption]
           [java.io FileNotFoundException]))

(def sep (System/getProperty "file.separator"))

(def version "1.8.9")

(def release (str (System/getProperty "user.home") sep ".m2" sep "repository" sep "org" sep "meyvn" sep "meyvn" sep version sep "meyvn-" version ".jar"))

(defn os-windows? []
  (str/starts-with? (str/lower-case (System/getProperty "os.name")) "windows"))

(defn maven-path []
  (let [cmd (if (os-windows?) ["where" "mvn"] ["which" "mvn"])
        pb (ProcessBuilder. cmd)
        process (.start pb)
        rc (.waitFor process)]
    (if (= rc 0)
       (let [path (-> (.getInputStream process)
                     slurp
                     str/split-lines  ; split on newlines
                     first            ; take only the first match
                     str/trim         ; clean any whitespace/CR
                     (Paths/get (into-array String [])))]
         (.toRealPath path (into-array LinkOption [])))
      (exit "Maven executable not found. Please install Maven prior to Meyvn." :status 1))))

(defn maven-home []
  (-> (maven-path) .getParent .getParent))

(defn bin-path []
  (let [path-sep (System/getProperty "path.separator") ; ":" or ";"
        path (-> (System/getenv "PATH") (str/split (re-pattern path-sep)))
        homedir (System/getProperty "user.home")
        candidates (if (os-windows?)
                     #{(str homedir sep "AppData" sep "Local" sep "Microsoft" sep "WindowsApps")
                       (str homedir sep "scoop" sep "shims")
                       (str homedir sep ".local" sep "bin")}
                     #{(str homedir sep ".local" sep "bin")
                       "/usr/local/bin"
                       (str homedir sep "bin")})
        exists #(.isDirectory (io/file %))
        selected (first (filter (every-pred candidates exists) path))]
    (println "Installation directory:" selected)
    selected))

(defn mvn-executable []
  (if (os-windows?) ["cmd" "/c" "mvn"] ["mvn"]))

(defn download []
  (let [pb (ProcessBuilder. (into (mvn-executable) ["org.apache.maven.plugins:maven-dependency-plugin:3.10.0:get" (str "-Dartifact=org.meyvn:meyvn:" version)]))
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

(defn sudo-write [path]
  (if (os-windows?)
    (exit "Insufficient permissions. Please re-run as Administrator." :status 1)
    (let [cmd ["/bin/bash" "-c" (str "/usr/bin/sudo -S /usr/bin/cp -p " (System/getProperty "java.io.tmpdir") sep "myvn " path " 2>&1")]
          pb (ProcessBuilder. cmd)
          process (.start pb)
          buffer (char-array 512)
          prompt-password (fn [s] (.readPassword (System/console) "%s" (into-array Object [s])))]
      (with-open [out (clojure.java.io/reader (.getInputStream process))
                  in (clojure.java.io/writer (.getOutputStream process))]
        (let [size (.read out buffer 0 512)]
          (when (clojure.string/includes? (clojure.string/join buffer) "[sudo] password")
            (when-let [password (prompt-password (String/valueOf buffer 0 size))]
              (.write in password 0 (count password))
              (.newLine in)
              (.flush in))))))))

(defn launcher-content [home]
  (if (os-windows?)
    (str "@echo off\r\njava -Dmaven.home=" home " -jar " release " %*\r\n")
    (str "java -Dmaven.home=" home " -jar " release " $@\n")))

(defn launcher-filename []
  (if (os-windows?) "myvn.cmd" "myvn"))

(defn -main [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options :in-order true)
        home (maven-home)
        sh (io/file (str (bin-path) (System/getProperty "file.separator") (launcher-filename)))]
    (when (:help options) (exit (usage summary)))
    (when (pos? (:verbose options)) (println "options: " options "\narguments: " arguments "\nerrors: " errors))
    (if (find-file release)
      (println "Existing Meyvn jar found.")
      (download))
    (println "can write?" (.canWrite (.getParentFile sh)))
    (if (.canWrite (.getParentFile sh))
      (do
        (spit sh (launcher-content home))
        (when-not (os-windows?) (.setExecutable sh true)))
      (let [sh (io/file (str (System/getProperty "java.io.tmpdir") sep (launcher-filename)))]
        (spit sh (launcher-content home))
        (.setExecutable sh true)
        (sudo-write (bin-path))))
    (println (str "`myvn' has been successfully installed in " (bin-path) "." ))))
