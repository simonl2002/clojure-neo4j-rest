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

(deftest rel-stuff
  (testing "relationship goodness"
    (let [root (db-root *root-url*)
          node0 (create-node root { :name "node0" :value "0"})
          node1 (create-node root { :name "node1" :value "1"})
          rel0 (create-relationship node0 node1 "loves" { :intensity "high" })
          rel1 (create-relationship node1 node0 "hates" {})]
      (is (not (nil? rel0)))
      (is (not (nil? rel1)))
      (is (= node0 (start-node rel0)))
      (is (= node1 (end-node rel0)))
      (is (= node1 (start-node rel1)))
      (is (= node0 (end-node rel1)))
      (is (= "high" (get-property rel0 "intensity")))
      (is (= 2 (count (relationships node0 All))))
      (is (= 1 (count (relationships node0 Out))))
      (is (true? (delete-relationship rel0)))
      (is (true? (delete-relationship rel1)))
      (is (true? (delete-node node0)))
      (is (true? (delete-node node1))))))

