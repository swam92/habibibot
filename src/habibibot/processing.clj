(ns habibibot.processing
  (:require [clojure.core.async :as async :refer
             [go go-loop chan buffer >! >!! <! <!! put! take! poll! close!]]
            [amazonica.aws.s3 :as s3 :refer [put-object]]
            [clojure.java.jdbc :as jdbc]
            [clojure.java.io :as io]))
            
;; consume from each chan, downloading object & writing to s3
;; update map with s3 urls
;; place on output chan
;; one by one or wait-and-batch from output chan to db

;;TODO: read from environment vars / slurp from config
(def cred {:access-key ""
           :secret-key ""
           :endpoint   ""})

(def db-props {:dbtype ""
               :dbname ""
               :host ""
               :user ""
               :password ""
               })

(def input-chan (chan (buffer 700)))
(def output-chan (chan (buffer 1000)))

(defn rand-str [len]
  (clojure.string/lower-case (apply str (take len (repeatedly #(char (+ (rand 26) 65)))))))

(defn ts->sql [timestamp]
  (->> (/ timestamp 1000)
       (java.time.Instant/ofEpochSecond)
       (java.sql.Timestamp/from)))

(defn process-data-async [output-chan]
  (go-loop []
    (let [data (<! input-chan)
          fname (rand-str 16)]
      (with-open [x (io/input-stream (or (:image_fb_url data) (:video_fb_url data)))]
        (as-> (put-object cred
                             :bucket-name "habibibot"
                             :key fname
                             :input-stream x) $
          (:etag $)
          (assoc data :s3_id fname :content_hash $)
          (update $ :timestamp #(ts->sql %))
          (clojure.set/rename-keys $ {:timestamp :send_time})
          (put! output-chan $))))
    (recur)))

(defn load-db []
  (jdbc/insert-multi! db-props
                      :content
                      (loop [data []]
                        (let [queue-contents (poll! output-chan)]
                          (if (nil? queue-contents)
                            data
                            (recur (conj data queue-contents)))))))

(process-data-async output-chan)




