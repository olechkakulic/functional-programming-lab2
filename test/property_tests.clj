(ns property-tests
  (:require [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]
            [sc-set :as set]))

(declare prop-add-then-find
         prop-remove-behaviour
         prop-monoid-identity
         prop-monoid-associative
         prop-map-values-composition)

(def pair-gen
  (gen/vector (gen/tuple gen/small-integer gen/small-integer)))

(defn build [pairs]
  (reduce (fn [acc [k v]] (set/add k v acc))
          (set/empty)
          pairs))

(defspec prop-add-then-find 200
  (prop/for-all [pairs pair-gen
                 k gen/small-integer
                 v gen/small-integer]
                (let [m  (build pairs)
                      m2 (set/add k v m)]
                  (= v (set/try-find k m2)))))

(defspec prop-remove-behaviour 200
  (prop/for-all [pairs pair-gen
                 k gen/small-integer
                 v gen/small-integer]
                (let [m        (build pairs)
                      m-added  (set/add k v m)
                      m-removed (set/remove k m-added)]
                  (and (nil? (set/try-find k m-removed))
                       (every? (fn [[kk _]]
                                 (if (= kk k)
                                   true
                                   (= (set/try-find kk m-removed) (set/try-find kk m))))
                               pairs)))))

(defspec prop-monoid-identity 200
  (prop/for-all [pairs pair-gen]
                (let [m (build pairs)
                      e (set/empty)]
                  (and (set/equals m (set/combine e m))
                       (set/equals m (set/combine m e))))))

(defspec prop-monoid-associative 200
  (prop/for-all [pa pair-gen
                 pb pair-gen
                 pc pair-gen]
                (let [a (build pa)
                      b (build pb)
                      c (build pc)
                      left  (set/combine a (set/combine b c))
                      right (set/combine (set/combine a b) c)]
                  (set/equals left right))))

(def funcs (gen/elements [inc dec (fn [x] (* 2 x)) identity]))

(defspec prop-map-values-composition 200
  (prop/for-all [pairs pair-gen
                 f funcs
                 g funcs]
                (let [m     (build pairs)
                      left  (set/map-values f (set/map-values g m))
                      right (set/map-values (comp f g) m)]
                  (set/equals left right))))
