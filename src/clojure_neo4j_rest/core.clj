(ns clojure-neo4j-rest.core
  (:require 
    [clj-http.client :as client]
    [clj-http.util :as http-util]
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

(defn- obj-url
  "returns the objs url"
  [obj]
  (get obj :self))
;
; Node funtions
;

(defn create-node 
  "Creates a node. on success returns the node, on fail returns nil"
  [dbroot properties]
  (let [node-url (dbroot :node)]
    (post-json node-url properties)))

(defn delete-node
  "delets a node. returns True if sucessful, false if not"
  [node]
  (try 
    (let [node-url (obj-url node)]
      (client/delete node-url)
      true)
    (catch Exception e false)))

;Property related functions
;They work on both nodes and relationships

(defn set-properties
  "set properties on a node. overwriting any that may already exist"
  [node properties]
  (let [prop-url (node :properties)]
    (put-json prop-url properties)))

(defn properties
  "gets the properties of a node/relationship"
  [obj]
  (let [prop-url (obj :properties)]
    (get-json prop-url)))

(defn delete-properties
  "deletes all the properties of a node"
  [obj]
  (try
    (let [prop-url (obj :properties)]
      (client/delete prop-url)
      true)
    (catch Exception e false)))

(defn get-property-url
  [obj property]
  (let [raw-url (obj :property)] 
    (string/replace-re #"\{.*\}" property raw-url)))

(defn get-property 
  "gets a property of a node"
  [obj property]
  (let [prop-url (get-property-url obj property)]
    (try
      (get-json prop-url)
      (catch Exception e nil))))

(defn set-property 
  "sets a property on a node"
  [obj property value]
  (let [prop-url (get-property-url obj property)]
    (put-json prop-url value)))

(defn delete-property 
  [obj property]
  (let [prop-url (get-property-url obj property)]
    (client/delete prop-url)))

; Relationship related functions

(defn create-relationship 
  "creates a relationshp between two nodes"
  [start-node end-node rel-type data]
  (let [rel-url (start-node :create_relationship)
        end-node-url (end-node :self)]
    (post-json rel-url { "to" end-node-url, "data" data, "type" rel-type})))

(defn delete-relationship
  "deletes a relationship"
  [rel]
  (try 
    (let [rel-url (rel :self)] 
      (client/delete rel-url)
      true)
    (catch Exception e false)))

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

;
; Index related functions
;

(defn create-node-index
  "creates a node index with the specified name and config"
  [dbroot name type provider]
  (let [index-url (dbroot :node_index)
        config { "type" type, "provider" provider }]
    (post-json index-url { "name" name, "config" config })))

(defn node-indices
  "gets existing node indices"
  [dbroot]
  (let [index-url (dbroot :node_index)] 
    (get-json index-url)))

(defn relationship-indices
  "returns existing relationship indices"
  [dbroot]
  (let [index-url (dbroot :relationship_index)]
    (get-json index-url)))

(defn get-node-index
  "gets a node index with the specified name"
  [dbroot name]
  (let [indices (node-indices dbroot)]
    (get indices (keyword name))))

(defn index-type
  "gets the type of an index"
  [index]
  (index :type))

(defn index-provider
  "returns the provider of the index"
  [index]
  (index :provider))

(defn- index-key-val-url
  [index the-key value]
  (let [template-url (index :template)
        key-sub-url (string/replace-re #"\{key\}" the-key template-url)]
        (string/replace-re #"\{value\}" (http-util/url-encode value) key-sub-url)))
  
(defn add-to-index
  "adds the item to the index"
  [index the-key value obj]
  (let [url (index-key-val-url index the-key value)]
    (post-json url (obj-url obj))))

(defn index-get
  "does exact match index query"
  [index the-key value]
  (let [url (index-key-val-url index the-key value)]
    (get-json url)))

