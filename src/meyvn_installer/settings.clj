(ns meyvn-installer.settings
  (:require [meyvn-installer.utils :refer [find-file]]
            [clojure.java.io :as io]
            [lang-utils.core :refer [seek]])
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
  (let [f user-settings]
    (if-let [settings-xml (find-file user-settings)]
      (let [settings (.read (DefaultSettingsReader.) settings-xml nil)]
        (SettingsUtils/merge settings (new-settings credentials) "user-level")
        (with-open [out (io/output-stream f)]
          (.write (DefaultSettingsWriter.) out nil settings)))
      (with-open [out (io/output-stream f)]
        (.write (DefaultSettingsWriter.) out nil (new-settings credentials))))))

(defn credentials-mismatch? [credentials]
  (if-let [settings-xml (find-file user-settings)]
    (let [settings (.read (DefaultSettingsReader.) settings-xml nil)
          servers (.getServers settings)]
      (if-let [server (seek #(= "meyvn" (.getId %)) servers)]
        (let [username (.getUsername server)
              password (.getPassword server)]
          (or (not= username (:user credentials)) (not= password (:pass credentials))))
        false))
    false))
