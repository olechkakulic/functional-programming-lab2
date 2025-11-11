(ns unit
  (:require [clojure.test :refer [deftest is]]
            [sc-set :as hm]
            [clojure.string :as str]))

(defn build [pairs]
  (reduce (fn [acc [k v]] (hm/add k v acc))
          (hm/empty)
          pairs))

(deftest add-and-try-find
  (let [m (build [[1 "one"] [2 "two"]])]
    (is (= "one" (hm/try-find 1 m)))
    (is (= "two" (hm/try-find 2 m)))))

(deftest remove-should-delete-key
  (let [m  (build [[1 "a"] [2 "b"]])
        m2 (hm/remove 1 m)]
    (is (nil? (hm/try-find 1 m2)))
    (is (= "b" (hm/try-find 2 m2)))))

(deftest contains-key-detects-presence-and-absence
  (let [m (build [[1 "a"] [2 "b"]])]
    (is (true? (hm/contains-key 1 m)))
    (is (false? (hm/contains-key 3 m)))))

(deftest filter-removes-nonmatching
  (let [m (build [[1 "one"] [2 "two"]])
        f (hm/filter (fn [k _] (zero? (mod k 2))) m)]
    (is (nil? (hm/try-find 1 f)))
    (is (= "two" (hm/try-find 2 f)))))

(deftest map-values-transforms-all-values
  (let [m (build [[1 "a"] [2 "b"]])
        m2 (hm/map-values str/upper-case m)]
    (is (= "A" (hm/try-find 1 m2)))
    (is (= "B" (hm/try-find 2 m2)))))

(deftest fold-accumulates-sum-of-keys
  (let [m (build [[1 "a"] [2 "b"] [3 "c"]])
        result (hm/fold (fn [acc k _] (+ acc k)) 0 m)]
    (is (= 6 result))))

(deftest combine-merges-and-overrides-duplicates
  (let [m1 (build [[1 "one"] [2 "two"]])
        m2 (build [[2 "TWO"] [3 "three"]])
        result (hm/combine m2 m1)]
    (is (= "one" (hm/try-find 1 result)))
    (is (= "TWO" (hm/try-find 2 result)))
    (is (= "three" (hm/try-find 3 result)))))

(deftest equals-compares-identical-maps
  (let [m1 (build [[2 "two"] [1 "one"]])
        m2 (build [[1 "one"] [2 "two"]])]
    (is (true? (hm/equals m2 m1)))))

(deftest equals-detects-different-values
  (let [m1 (build [[1 "one"] [2 "two"]])
        m2 (build [[1 "one"] [2 "TWO"]])]
    (is (false? (hm/equals m2 m1)))))

(deftest monoid-identity-with-empty
  (let [empty-monoid (hm/empty)
        m (build [[1 "a"] [2 "b"]])]
    (is (true? (hm/equals m (hm/combine empty-monoid m))))
    (is (true? (hm/equals m (hm/combine m empty-monoid))))))
