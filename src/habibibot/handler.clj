(ns habibibot.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer :all]
            [ring.middleware.json :refer [wrap-json-params wrap-json-body]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer :all]
            [clojure.walk :as w]
            [clojure.data.json :as json]
            [clojure.core.async :refer [put!]]
            [habibibot.processing :as proc :refer [input-chan]])
  (:use ring.util.response
        ring.adapter.jetty))

(defn get-key [prefix key]
  (if (nil? prefix)
    key
    (str prefix "_" key)))

(defn flatten-map-kvs
    ([map] (flatten-map-kvs map nil))
    ([map prefix]
     (reduce
         (fn [item [k v]]
             (if (map? v)
                (concat item (flatten-map-kvs v (get-key prefix (name k))))
                (conj item [(get-key prefix (name k)) v])))
         [] map)))

(defn flatten-map [m]
  (into {} (flatten-map-kvs m)))

(defn parse-request [request]
  (let [params (-> request
                   (:body)
                   (slurp)
                   (json/read-str ,,, :key-fn keyword))
        parsed-request (assoc request :params params)]
    parsed-request))

(defn handle-payload [parsed-request]
  ;; either returns attachments or nil
  (let [send-details (-> (get-in parsed-request [:params :entry 0 :messaging 0])
                         (dissoc ,,, :message)
                         (flatten-map-kvs)
                         (flatten-map)
                         (w/keywordize-keys))
        message (get-in parsed-request [:params :entry 0 :messaging 0 :message])
        base-message (dissoc message :attachments)
        get-attachment (fn [x] (get-in x [:payload :url]))]
    
    (when-not (nil? (:attachments message))
      ;;TODO: refactor this so it doesn't suck
      (map #(case (:type %)
              "image" (->> (merge send-details
                                  base-message
                                  (assoc {:video_fb_url nil} :image_fb_url (get-attachment %)))
                           (put! input-chan))
              "video" (->> (merge send-details
                                  base-message
                                  (assoc {:image_fb_url nil} :video_fb_url (get-attachment %)))
                           (put! input-chan)))
           (:attachments message)))))

(defroutes app-routes
  (GET "/" request (-> request
                       (assoc-query-params ,,, "UTF-8")
                       (:query-params)
                       (w/keywordize-keys)
                       (:hub.challenge)))

  (POST "/" request (let [x (parse-request request)
                          t (handle-payload x)]
                      (do (clojure.pprint/pprint t)
                          t
                          (response "fuk u mark")
                          #_(-> t
                                   (json/write-str)
                                   (response)
                                   (content-type ,,, "application/json"))
                          ))))

(def app
  (-> app-routes
      (wrap-defaults ,,, site-defaults)
      (wrap-keyword-params)
      (wrap-json-params)))

;; code for ring+lambda+api-gateway library
#_(defhandler habibibot.handler.app
  (-> app-routes
      (wrap-defaults ,,, site-defaults)
      (wrap-keyword-params)
      (wrap-json-params)))

;; example from ring+lambda library
#_(defhandler example.lambda.MyLambdaFn (wrap-json-response
                                       (wrap-json-params
                                        (wrap-params
                                         (wrap-keyword-params
                                          app))))
                                      {})

;; for testing locally via jetty
#_(defonce server (run-jetty app-routes {:port 3000 :join? false}))
