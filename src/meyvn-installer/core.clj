(ns meyvn-installer.core
  (:gen-class)
  (:require [clojure.string :as str :refer [trim-newline]]
            [meyvn-installer.utils :refer [exit find-file]]
            [clojure.java.io :as io])
  (:import [java.nio.file Path LinkOption]
           [org.apache.maven.settings Settings Server SettingsUtils]
           [org.apache.maven.settings.io DefaultSettingsWriter DefaultSettingsReader]))


;; add username and password to settings.xml
;; Check if maven is installed


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


(def credentials {:user "danielsz"
                  :pass "rF2eg1gngXcFv9vzmf6hlFucL"})

(defn new-settings []
  (let [settings (Settings.)]
    (let [server (doto (Server.)
                   (.setId "meyvn")
                   (.setUsername (:user credentials))
                   (.setPassword (:pass credentials)))]
      (doto settings        (.addServer server)))
    settings))

(def user-settings (str (System/getProperty "user.home") "/.m2/settings.xml"))

(defn write-settings []
  (let [f (str (System/getProperty "java.io.tmpdir") "/settings.xml")]
    (if-let [settings-xml (find-file user-settings)]
      (let [user-settings (.read (DefaultSettingsReader.) settings-xml nil)]
        (SettingsUtils/merge user-settings (new-settings) "user-level")
        (with-open [out (io/output-stream f)]
          (.write (DefaultSettingsWriter.) out nil user-settings))
        (io/file f))
      (with-open [out (io/output-stream f)]
        (.write (DefaultSettingsWriter.) out nil (new-settings))))
    f))

(defn download []
  (let [settings (write-settings)
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
      (download))
    (.setExecutable sh true)
    (spit sh (str "M2_HOME=" home " java -jar " meyvn " $@"))))
