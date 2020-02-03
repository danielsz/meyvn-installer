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

(defn pwd-prompt []
  (let [console (System/console)
        password (.readPassword console "Password: " nil)]
    (str/join password)))

(defn confirm [message]
  (let [_ (println (str message " Y(es) or N(o)?"))
        option (read-line)]
    (.startsWith (str/lower-case option) "y")))

