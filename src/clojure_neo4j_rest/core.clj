(ns clojure-neo4j-rest.core
    (:require 
        [clj-http.client :as client]
        [clojure.contrib.json :as json]))

(def *neo-host* "192.168.1.111:7474")
(def *path-base* "db/data")

(defn get-json [url]
    (let [response (client/get url {:accept :json})]
          (json/read-json (response :body))))

(defn post-json [url data]
    (let [response (client/post url {:accept :json :content-type :json :body (json/json-str data)})]
        (json/read-json (response :body))))

(defn db-root
    "gets the neo4j databse root"
    [db-url]
    (get-json db-url))

(defn reference-node
    "returns the reference node for the db"
    [dbroot]
    (let [ref-node-url (dbroot :reference_node)]
        (get-json ref-node-url)))

(defn create-node [dbroot properties]
    (let [node-url (dbroot :node)]
        (post-json node-url properties)))
