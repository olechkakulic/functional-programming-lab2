;; ;; test/property_tests.clj
;; (ns property-tests
;;   (:require [clojure.test :refer :all]
;;             [clojure.test.check.generators :as gen]
;;             [clojure.test.check.properties :as prop]
;;             [clojure.test.check.clojure-test :as ctc :refer [defspec]]
;;             [sc-set :as hm]))


;; (def pair-gen
;;   (gen/vector (gen/tuple gen/int gen/string-alphanumeric)))

;; ;; 1) Identity (left and right)
;; (defspec combine-identity 200
;;   (prop/for-all [pairs pair-gen]
;;     (let [build (fn [ps] (reduce (fn [acc [k v]] (hm/add k v acc)) (hm/empty) ps))
;;           m (build pairs)]
;;       (and (hm/equals (hm/combine (hm/empty) m) m)
;;            (hm/equals (hm/combine m (hm/empty)) m)))))

;; ;; 2) Associativity
;; (defspec combine-associative 200
;;   (prop/for-all [a pair-gen b pair-gen c pair-gen]
;;     (let [build (fn [ps] (reduce (fn [acc [k v]] (hm/add k v acc)) (hm/empty) ps))
;;           A (build a) B (build b) C (build c)]
;;       (hm/equals (hm/combine A (hm/combine B C))
;;                  (hm/combine (hm/combine A B) C)))))

;; ;; 3) add/remove property
;; (defspec add-remove 200
;;   (prop/for-all [k gen/int v gen/string-alphanumeric ps pair-gen]
;;     (let [build (fn [ps] (reduce (fn [acc [k v]] (hm/add k v acc)) (hm/empty) ps))
;;           base (build ps)
;;           m (-> base (hm/add k v) (hm/remove k))]
;;       (not (hm/contains-key k m)))))
