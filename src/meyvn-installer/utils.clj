(ns meyvn-installer.utils
  (:require [clojure.java.io :as io]))

(defn find-file [s]
  (let [f (io/file s)]
    (when (.exists f )
      f)))

(defn exit [msg & {:keys [status] :or {status 0}}]
  (println msg)
  (System/exit status))
