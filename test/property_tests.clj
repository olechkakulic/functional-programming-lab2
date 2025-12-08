(ns property-tests

  (:require [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]
            [sc-set :as set]))

(declare prop-add-then-contains
         prop-remove-behaviour
         prop-monoid-identity
         prop-monoid-associative
         prop-map-composition)

(def elem-gen gen/small-integer)
(def elems-gen (gen/vector elem-gen))

(defn build
  [elems]
  (reduce (fn [acc e] (set/add e acc))
          (set/empty)
          elems))

(def set-gen (gen/fmap build elems-gen))
(defspec prop-add-then-contains 200
  (prop/for-all [elems elems-gen
                 x elem-gen]
                (let [s  (build elems)
                      s2 (set/add x s)]
                  (set/contains-element x s2))))

(defspec prop-remove-behaviour 200
  (prop/for-all [elems elems-gen
                 x elem-gen]
                (let [s         (build elems)
                      s-added   (set/add x s)
                      s-removed (set/remove x s-added)]
                  (and (not (set/contains-element x s-removed))
                       (every? (fn [y]
                                 (if (= y x)
                                   true
                                   (= (set/contains-element y s)
                                      (set/contains-element y s-removed))))
                               elems)))))

(defspec prop-monoid-identity 200
  (prop/for-all [s set-gen]
                (let [e (set/empty)]
                  (and (set/equals s (set/combine e s))
                       (set/equals s (set/combine s e))))))

(defspec prop-monoid-associative 200
  (prop/for-all [a set-gen
                 b set-gen
                 c set-gen]
                (let [left  (set/combine a (set/combine b c))
                      right (set/combine (set/combine a b) c)]
                  (set/equals left right))))
(def funcs
  (gen/elements [inc dec (fn [x] (* 2 x)) identity]))

(defspec prop-map-composition 200
  (prop/for-all [elems elems-gen
                 f funcs
                 g funcs]
                (let [s     (build elems)
                      left  (set/map f (set/map g s))
                      right (set/map (comp f g) s)]
                  (set/equals left right))))
