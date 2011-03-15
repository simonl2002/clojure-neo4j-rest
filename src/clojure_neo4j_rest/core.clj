(ns clojure-neo4j-rest.core
    (:require 
        [clj-http.client :as client]
        [clojure.contrib.json :as json]
        [clojure.contrib.string :as string]))

(defn get-json [url]
    (let [response (client/get url {:accept :json})
          body (response :body)]
        (if (nil? body)
            nil
            (json/read-json body))))

(defn post-json 
    "will post the data as json. will return json response data or nil"
    [url data]
    (try 
        (let [response (client/post url {:accept :json :content-type :json :body (json/json-str data)})]
            (json/read-json (response :body)))
        (catch Exception e nil)))

(defn put-json
    [url data]
    (try
        (let [response (client/put url {:accept :json :content-type :json :body (json/json-str data)})
              body (response :body)]
            (if (nil? body) true (json/read-json body)))
        (catch Exception e nil)))

(defn- get-node [url]
    (get-json url))

(defn db-root
    "gets the neo4j databse root"
    [db-url]
    (get-node db-url))

(defn reference-node
    "returns the reference node for the db"
    [dbroot]
    (let [ref-node-url (dbroot :reference_node)]
        (get-node ref-node-url)))

(defn create-node 
    "Creates a node. on success returns the node, on fail returns nil"
    [dbroot properties]
    (let [node-url (dbroot :node)]
        (post-json node-url properties)))

(defn delete-node
    "delets a node. returns True if sucessful, false if not"
    [node]
    (try 
        (let [node-url (node :self)]
            (client/delete node-url)
            true)
        (catch Exception e false)))

(defn set-properties
    "set properties on a node. overwriting any that may already exist"
    [node properties]
    (let [prop-url (node :properties)]
        (put-json prop-url properties)))

(defn properties
    "gets the properties of a node"
    [node]
    (let [prop-url (node :properties)]
        (get-json prop-url)))

(defn delete-properties
    "deletes all the properties of a node"
    [node]
    (try
        (let [prop-url (node :properties)]
            (client/delete prop-url)
            true)
        (catch Exception e false)))

;(def get-property [node property])
;(def set-property [node property value])
;(def delete-property [node property])

(def All :all)
(def In :in)
(def Out :out)

(defn relationships
    "get the relationships for a node"
    ([node direction] (relationships node direction nil))
    ([node direction types]
        (let [no-types (empty? types)
            type-string (string/join "&" types)
            rel-url 
            (cond 
                (and no-types (= All direction)) (node :all_relationships)
                (= All direction) (string/replace-re #"\{.*\}" type-string (node :all_typed_relationships))
                (and no-types (= In direction)) (node :incoming_relationships)
                (= In direction) (string/replace-re #"\{.*\}" type-string (node :incoming_typed_relationships))
                (and no-types (= Out direction)) (node :outgoing_relationships)
                (= Out direction) (string/replace-re #"\{.*}" type-string (node :outgoing_typed_relationships)))]
            (get-json rel-url))))


(defn get-relationship-node
    "gets a particular node on a relationship"
    [relationship side]
    (let [node-url (relationship side)]
        (get-json node-url)))

(defn end-node
    "gets the end node of a relationship"
    [relationship]
    (get-relationship-node relationship :end))

(defn start-node
    "gets that start node of a relationship"
    [relationship]
    (get-relationship-node relationship :start))
