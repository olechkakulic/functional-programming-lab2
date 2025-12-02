(ns sc-set
  (:refer-clojure :exclude [empty remove filter map]))

(def ^:private default-capacity 16)

(defn empty
  []
  {:buckets (vec (repeat default-capacity []))
   :capacity default-capacity})

(defn- bucket-index
  [capacity e]
  (mod (hash e) capacity))

(defn- update-bucket
  [s e f]
  (let [{:keys [buckets capacity]} s
        idx (bucket-index capacity e)]
    (update s :buckets
            (fn [buckets]
              (update buckets idx f)))))

(defn- without-elem
  [e bucket]
  (vec (clojure.core/remove #(= % e) bucket)))

(defn add
  [e s]
  (update-bucket s e
                 (fn [bucket]
                   (conj (without-elem e bucket) e))))

(defn remove
  [e s]
  (update-bucket s e
                 (fn [bucket]
                   (without-elem e bucket))))

(defn contains-element
  [e s]
  (let [{:keys [buckets capacity]} s
        idx (bucket-index capacity e)
        bucket (nth buckets idx)]
    (boolean (some #(= % e) bucket))))

(defn filter
  [pred s]
  (update s :buckets
          (fn [buckets]
            (mapv (fn [bucket]
                    (vec (clojure.core/filter pred bucket)))
                  buckets))))

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
  (let [all-in? (fn [x y]
                  (fold (fn [ok e]
                          (and ok (contains-element e y)))
                        true
                        x))]
    (and (all-in? a b)
         (all-in? b a))))
