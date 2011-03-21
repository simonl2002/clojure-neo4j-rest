(ns clojure-neo4j-rest.test.core
  (:use [clojure-neo4j-rest.core] :reload)
  (:use [clojure.test]))

(def *root-url* "http://localhost:7474/db/data")

(deftest root-test
  (let [root (db-root *root-url*)]
    (testing "root and reference node functions"
      (is (not (nil? root)) "testing db root retrieval")
      (is (not (nil? (reference-node root))) "testing reference-node fetching"))))

(deftest node-stuff
  (testing "node manipulation"
    (let [root (db-root *root-url*)
          node (create-node root {"name" "bob", "value" "tEst"})]
      (is (not (nil? node)))
      (is (= "tEst" (get-property node "value")))
      (is (= "bob" (get-property node "name")))
      (is (nil? (get-property node "fake-property")) "getting non-existent property")
      (set-property node "value" "9999")
      (is (= "9999" (get-property node "value")))
      (is (= "9999" (get (properties node) :value)) "testing getting all properties")
      (is (true? (delete-properties node)))
      (is (empty? (properties node)))
      (is (true? (delete-node node))))))
