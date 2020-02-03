(ns meyvn-installer.settings
  (:require [meyvn-installer.utils :refer [find-file]]
            [clojure.java.io :as io])
  (:import [org.apache.maven.settings Settings Server SettingsUtils]
           [org.apache.maven.settings.io DefaultSettingsWriter DefaultSettingsReader]))

(def user-settings (str (System/getProperty "user.home") "/.m2/settings.xml"))

(defn new-settings [credentials]
  (let [settings (Settings.)]
    (let [server (doto (Server.)
                   (.setId "meyvn")
                   (.setUsername (:user credentials))
                   (.setPassword (:pass credentials)))]
      (doto settings        (.addServer server)))
    settings))

(defn write-settings [credentials]
  (let [f (str (System/getProperty "java.io.tmpdir") "/settings.xml")]
    (if-let [settings-xml (find-file user-settings)]
      (let [user-settings (.read (DefaultSettingsReader.) settings-xml nil)]
        (SettingsUtils/merge user-settings (new-settings credentials) "user-level")
        (with-open [out (io/output-stream f)]
          (.write (DefaultSettingsWriter.) out nil user-settings))
        (io/file f))
      (with-open [out (io/output-stream f)]
        (.write (DefaultSettingsWriter.) out nil (new-settings))))
    f))
