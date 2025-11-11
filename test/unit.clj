(ns unit
  (:require [clojure.test :refer [deftest is]]
            [sc-set :as set]
            [clojure.string :as str]))

(defn build [pairs]
  (reduce (fn [acc [k v]] (set/add k v acc))
          (set/empty)
          pairs))

(deftest add-and-try-find
  (let [m (build [[1 "one"] [2 "two"]])]
    (is (= "one" (set/try-find 1 m)))
    (is (= "two" (set/try-find 2 m)))))

(deftest remove-should-delete-key
  (let [m  (build [[1 "a"] [2 "b"]])
        m2 (set/remove 1 m)]
    (is (nil? (set/try-find 1 m2)))
    (is (= "b" (set/try-find 2 m2)))))

(deftest contains-key-detects-presence-and-absence
  (let [m (build [[1 "a"] [2 "b"]])]
    (is (true? (set/contains-key 1 m)))
    (is (false? (set/contains-key 3 m)))))

(deftest filter-removes-nonmatching
  (let [m (build [[1 "one"] [2 "two"]])
        f (set/filter (fn [k _] (zero? (mod k 2))) m)]
    (is (nil? (set/try-find 1 f)))
    (is (= "two" (set/try-find 2 f)))))

(deftest map-values-transforms-all-values
  (let [m (build [[1 "a"] [2 "b"]])
        m2 (set/map-values str/upper-case m)]
    (is (= "A" (set/try-find 1 m2)))
    (is (= "B" (set/try-find 2 m2)))))

(deftest fold-accumulates-sum-of-keys
  (let [m (build [[1 "a"] [2 "b"] [3 "c"]])
        result (set/fold (fn [acc k _] (+ acc k)) 0 m)]
    (is (= 6 result))))

(deftest combine-merges-and-overrides-duplicates
  (let [m1 (build [[1 "one"] [2 "two"]])
        m2 (build [[2 "TWO"] [3 "three"]])
        result (set/combine m2 m1)]
    (is (= "one" (set/try-find 1 result)))
    (is (= "TWO" (set/try-find 2 result)))
    (is (= "three" (set/try-find 3 result)))))

(deftest equals-compares-identical-maps
  (let [m1 (build [[2 "two"] [1 "one"]])
        m2 (build [[1 "one"] [2 "two"]])]
    (is (true? (set/equals m2 m1)))))

(deftest equals-detects-different-values
  (let [m1 (build [[1 "one"] [2 "two"]])
        m2 (build [[1 "one"] [2 "TWO"]])]
    (is (false? (set/equals m2 m1)))))

(deftest monoid-identity-with-empty
  (let [empty-monoid (set/empty)
        m (build [[1 "a"] [2 "b"]])]
    (is (true? (set/equals m (set/combine empty-monoid m))))
    (is (true? (set/equals m (set/combine m empty-monoid))))))
