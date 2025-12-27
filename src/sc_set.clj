(ns sc-set
  (:refer-clojure :exclude [empty remove filter map]))

(def ^:private default-capacity 16)

(def ^:private load-factor 0.75)

(defn- new-buckets
  [capacity]
  (vec (repeat capacity [])))

(defn empty
  []
  {:buckets  (new-buckets default-capacity)
   :capacity default-capacity
   :size     0})

(defn- bucket-index
  [capacity e]
  (mod (hash e) capacity))

(defn- threshold
  [capacity]
  (max 1 (int (Math/ceil (* capacity load-factor)))))

(defn- rehash
  [s new-capacity]
  (let [rebuilt (reduce
                 (fn [buckets bucket]
                   (reduce (fn [b e]
                             (let [idx (bucket-index new-capacity e)]
                               (update b idx conj e)))
                           buckets
                           bucket))
                 (new-buckets new-capacity)
                 (:buckets s))]
    (assoc s :buckets rebuilt :capacity new-capacity)))

(defn- ensure-capacity
  [s]
  (let [{:keys [size capacity]} s]
    (if (>= size (threshold capacity))
      (rehash s (* 2 capacity))
      s)))

(defn- without-elem
  [e bucket]
  (vec (clojure.core/remove #(= % e) bucket)))

(defn add
  [e s]
  (let [{:keys [buckets capacity size]} s
        idx (bucket-index capacity e)
        bucket (nth buckets idx)]
    (if (some #(= % e) bucket)
      s
      (-> s
          (assoc :buckets (assoc buckets idx (conj bucket e)))
          (assoc :size (inc size))
          (ensure-capacity)))))

(defn remove
  [e s]
  (let [{:keys [buckets capacity size]} s
        idx (bucket-index capacity e)
        bucket (nth buckets idx)
        new-bucket (without-elem e bucket)
        removed? (< (count new-bucket) (count bucket))]
    (cond-> s
      true (assoc :buckets (assoc buckets idx new-bucket))
      removed? (assoc :size (dec size)))))

(defn contains-element
  [e s]
  (let [{:keys [buckets capacity]} s
        idx (bucket-index capacity e)
        bucket (nth buckets idx)]

    (boolean (some #(= % e) bucket))))

(defn size
  [s]
  (:size s))

(defn filter
  [pred s]
  (let [buckets (:buckets s)
        new-buckets (mapv (fn [bucket]
                            (vec (clojure.core/filter pred bucket)))
                          buckets)
        new-size (reduce + (clojure.core/map count new-buckets))]
    (assoc s :buckets new-buckets :size new-size)))

(defn fold
  [f init s]
  (let [{:keys [buckets]} s]
    (reduce (fn [acc bucket]
              (reduce f acc bucket))
            init
            buckets)))

(defn fold-back
  [f init s]
  (let [{:keys [buckets]} s
        rev-buckets (reverse buckets)]
    (reduce (fn [acc bucket]
              (reduce (fn [acc' e] (f e acc')) acc (reverse bucket)))
            init
            rev-buckets)))

(defn map
  [f s]
  (fold (fn [acc e] (add (f e) acc))
        (empty)
        s))

(defn combine
  [s1 s2]
  (fold (fn [acc e] (add e acc)) s2 s1))

(defn equals
  [a b]
  (if (not= (size a) (size b))
    false
    (loop [elems []
           buckets (:buckets a)]
      (cond
        (seq elems) (if (contains-element (first elems) b)
                      (recur (rest elems) buckets)
                      false)
        (seq buckets) (recur (first buckets) (rest buckets))
        :else true))))
