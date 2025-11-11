(ns property-tests
  (:require [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]
            [sc-set :as hm]))

(declare prop-add-then-find
         prop-remove-behaviour
         prop-monoid-identity
         prop-monoid-associative
         prop-map-values-composition)

(def pair-gen
  (gen/vector (gen/tuple gen/small-integer gen/small-integer)))

(defn build [pairs]
  (reduce (fn [acc [k v]] (hm/add k v acc))
          (hm/empty)
          pairs))

(defspec prop-add-then-find 200
  (prop/for-all [pairs pair-gen
                 k gen/small-integer
                 v gen/small-integer]
                (let [m  (build pairs)
                      m2 (hm/add k v m)]
                  (= v (hm/try-find k m2)))))

(defspec prop-remove-behaviour 200
  (prop/for-all [pairs pair-gen
                 k gen/small-integer
                 v gen/small-integer]
                (let [m        (build pairs)
                      m-added  (hm/add k v m)
                      m-removed (hm/remove k m-added)]
                  (and (nil? (hm/try-find k m-removed))
                       (every? (fn [[kk _]]
                                 (if (= kk k)
                                   true
                                   (= (hm/try-find kk m-removed) (hm/try-find kk m))))
                               pairs)))))

(defspec prop-monoid-identity 200
  (prop/for-all [pairs pair-gen]
                (let [m (build pairs)
                      e (hm/empty)]
                  (and (hm/equals m (hm/combine e m))
                       (hm/equals m (hm/combine m e))))))

(defspec prop-monoid-associative 200
  (prop/for-all [pa pair-gen
                 pb pair-gen
                 pc pair-gen]
                (let [a (build pa)
                      b (build pb)
                      c (build pc)
                      left  (hm/combine a (hm/combine b c))
                      right (hm/combine (hm/combine a b) c)]
                  (hm/equals left right))))

(def funcs (gen/elements [inc dec (fn [x] (* 2 x)) identity]))

(defspec prop-map-values-composition 200
  (prop/for-all [pairs pair-gen
                 f funcs
                 g funcs]
                (let [m     (build pairs)
                      left  (hm/map-values f (hm/map-values g m))
                      right (hm/map-values (comp f g) m)]
                  (hm/equals left right))))
