(ns meyvn-installer.core
  (:gen-class)
  (:require [clojure.string :as str :refer [trim-newline]]
            [meyvn-installer.utils :as utils :refer [exit find-file]]
            [meyvn-installer.settings :refer [write-settings]]
            [clojure.java.io :as io]
            [clojure.java.browse :refer [browse-url]])
  (:import [java.nio.file Path LinkOption]))

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

(defn bin-path []
  (let [path (-> (System/getenv "PATH")
                (str/split #":"))
        homedir (System/getProperty "user.home")
        candidates #{(str homedir "/.local/bin") (str homedir "/bin") "/usr/local/bin"}]
    (some candidates path)))

(defn download [credentials]
  (let [settings (write-settings credentials)
        pb (ProcessBuilder. ["mvn" "-s" settings "org.apache.maven.plugins:maven-dependency-plugin:2.10:get" "-DremoteRepositories=meyvn::::https://nexus.tuppu.net/repository/meyvn/" "-Dartifact=org.danielsz:meyvn:1.3.4"])
        rc (.waitFor (-> pb .start))]
    (if (zero? rc)
      (println "Finished downloading")
      (exit "There was a problem downloading meyvn." :status 1))))

(defn -main [& args]
  (let [home (maven-home)
        path (bin-path)
        meyvn (str (System/getProperty "user.home") "/.m2/repository/org/danielsz/meyvn/1.3.4/meyvn-1.3.4.jar")
        sh (io/file (str path "/myvn"))]
    (if (find-file meyvn)
      (println "Found meyvn jar.")
      (if (utils/confirm "You will need the username/password that came with your licence. Are you ready to proceed?")
    (let [username (.readLine (System/console) "Username: " (into-array Object []))
          password (utils/pwd-prompt)]
      (download {:user username :pass password})
      (.setExecutable sh true)
      (spit sh (str "M2_HOME=" home " java -jar " meyvn " $@"))
      (println "The \"myvn\" binary is now in your path. Meyvn has been successfully installed."))
    (if (utils/confirm "Would you like to apply for a licence?")
      (do (browse-url "https://meyvn.org")
          (exit "Thank you!"))
      (exit "Bye for now."))))))
