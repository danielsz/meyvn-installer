(ns meyvn-installer.core
  (:gen-class)
  (:require [clojure.string :as str :refer [trim-newline]]
            [meyvn-installer.utils :as utils :refer [exit find-file]]
            [meyvn-installer.settings :refer [write-settings]]
            [clojure.java.io :as io]
            [clojure.java.browse :refer [browse-url]])
  (:import [java.nio.file Paths LinkOption]
           [java.io FileNotFoundException]))

(def version "1.3.6")
(def release (str (System/getProperty "user.home") "/.m2/repository/org/danielsz/meyvn/" version "/meyvn-" version ".jar"))

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
        candidates #{(str homedir "/.local/bin") (str homedir "/bin") "/usr/local/bin"}]
    (some candidates path)))

(defn download [credentials]
  (let [settings (write-settings credentials)
        pb (ProcessBuilder. ["mvn" "-s" settings "org.apache.maven.plugins:maven-dependency-plugin:2.10:get" "-DremoteRepositories=meyvn::::https://nexus.tuppu.net/repository/meyvn/" (str "-Dartifact=org.danielsz:meyvn:" version)])
        rc (.waitFor (-> pb .inheritIO .start))]
    (if (zero? rc)
      (println "Finished downloading")
      (exit "There was a problem downloading meyvn." :status 1))))

(defn -main [& args]
  (let [home (maven-home)
        sh (io/file (str (bin-path) "/myvn"))]
    (when (nil? (System/console)) (exit "Please run this program in your terminal. Thank you!"))
    (if (find-file release)
      (println "Meyvn jar is found.")
      (if (utils/confirm "You will need the username/password that came with your licence. Are you ready to proceed?")
        (let [username (.readLine (System/console) "Username: " (into-array Object []))
              password (utils/pwd-prompt)]
          (when (or (str/blank? username) (str/blank? password)) (exit "Username and password must be specified." :status 1))
          (download {:user username :pass password}))
        (if (utils/confirm "Would you like to apply for a licence?")
          (do (browse-url "https://meyvn.org")
              (exit "Thank you!"))
          (exit "Bye for now."))))
    (try
      (spit sh (str "M2_HOME=" home " java -jar " release " $@"))
      (.setExecutable sh true)
      (catch FileNotFoundException e
        (println (.getMessage e))
        (exit "Please consider having ~/bin or ~/.local/bin in your path rather than sudo'ing." :status 1)))
    (println "The \"myvn\" binary is now in your path. Meyvn has been successfully installed.")))
