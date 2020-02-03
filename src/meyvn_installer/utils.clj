(ns meyvn-installer.utils
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(defn find-file [s]
  (let [f (io/file s)]
    (when (.exists f )
      f)))

(defn exit [msg & {:keys [status] :or {status 0}}]
  (println msg)
  (System/exit status))

(defn prompt []
  (let [console (System/console)
        password (.readPassword console "Please enter your password: " nil)]
    (str/join password)))

(defn confirm [title message]
  (let [_ (println (str message " " title " (Please type Yes or No)"))
        option (read-line)]
    (when (not= (.startsWith (str/lower-case option) "y"))
      (throw (Exception. "User did not confirm")))))
