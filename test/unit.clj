(ns unit
  (:require [clojure.test :refer [deftest is]]
            [sc-set :as set]))

(defn build
  [elems]
  (reduce (fn [acc e] (set/add e acc))
          (set/empty)
          elems))

(deftest add-and-contains
  (let [s (build [1 2])]
    (is (true?  (set/contains-element 1 s)))
    (is (true?  (set/contains-element 2 s)))
    (is (false? (set/contains-element 3 s)))))

(deftest remove-should-delete-element
  (let [s  (build [1 2])
        s2 (set/remove 1 s)]
    (is (false? (set/contains-element 1 s2)))
    (is (true?  (set/contains-element 2 s2)))))

(deftest contains-element-detects-presence-and-absence
  (let [s (build [1 2])]
    (is (true?  (set/contains-element 1 s)))
    (is (true?  (set/contains-element 2 s)))
    (is (false? (set/contains-element 3 s)))))

(deftest filter-removes-nonmatching
  (let [s (build [1 2 3 4])
        f (set/filter even? s)]
    (is (false? (set/contains-element 1 f)))
    (is (false? (set/contains-element 3 f)))
    (is (true?  (set/contains-element 2 f)))
    (is (true?  (set/contains-element 4 f)))))

(deftest map-transforms-all-elements
  (let [s  (build [1 2 3])
        s2 (set/map #(* 2 %) s)]
    (is (true?  (set/contains-element 2 s2)))
    (is (true?  (set/contains-element 4 s2)))
    (is (true?  (set/contains-element 6 s2)))
    (is (false? (set/contains-element 1 s2)))))

(deftest fold-accumulates-sum-of-elements
  (let [s      (build [1 2 3])
        result (set/fold (fn [acc e] (+ acc e)) 0 s)]
    (is (= 6 result))))

(deftest combine-merges-as-union
  (let [s1 (build [1 2])
        s2 (build [2 3])
        r  (set/combine s1 s2)]
    (is (true?  (set/contains-element 1 r)))
    (is (true?  (set/contains-element 2 r)))
    (is (true?  (set/contains-element 3 r)))
    (is (false? (set/contains-element 4 r)))))

(deftest equals-compares-identical-sets
  (let [s1 (build [1 2 3])
        s2 (build [3 2 1 1])]
    (is (true? (set/equals s1 s2)))))

(deftest equals-detects-different-sets
  (let [s1 (build [1 2])
        s2 (build [1 3])]
    (is (false? (set/equals s1 s2)))))

;;  проверка свойства нейтрального элемента моноида
(deftest monoid-identity-with-empty
  (let [empty-set (set/empty)
        s         (build [1 2])]
    (is (true? (set/equals s (set/combine empty-set s))))
    (is (true? (set/equals s (set/combine s empty-set))))))

(deftest add-resizes-when-growing
  (let [elems (range 100)
        s     (build elems)]
    (is (every? #(set/contains-element % s) elems))
    (is (= (count elems) (set/size s)))))
