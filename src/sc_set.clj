(ns sc-set
  (:refer-clojure :exclude [filter empty remove]))

(defn empty
  []
  {:buckets (vec (repeat 16 []))
   :capacity 16})

(defn- bucket-index
  [capacity k]
  (mod (hash k) capacity))

(defn add
  [k value m]
  (let [{:keys [buckets capacity]} m
        idx (bucket-index capacity k)
        bucket (nth buckets idx)
        ;; создаём новый вектор filtered, который содержит все пары из bucket, кроме тех, у которых ключ равен k.
        filtered (->> bucket (clojure.core/remove (fn [[kk _]] (= kk k))) vec)
        updated-bucket (into [[k value]] filtered)
        new-buckets (assoc buckets idx updated-bucket)]
    (assoc m :buckets new-buckets)))

(defn remove
  [k m]
  (let [{:keys [buckets capacity]} m
        idx (bucket-index capacity k)
        updated-bucket (->> (nth buckets idx) (clojure.core/remove (fn [[kk _]] (= kk k))) vec)
        new-buckets (assoc buckets idx updated-bucket)]
    (assoc m :buckets new-buckets)))
;; ищем ключ по значению
(defn try-find
  [k m]
  (let [{:keys [buckets capacity]} m
        idx (bucket-index capacity k)]
    (some (fn [[kk v]] (when (= kk k) v)) (nth buckets idx))))

(defn contains-key
  [k m]
  (boolean (try-find k m)))

;; функц ко всем знач
(defn map-values
  [f m]
  (let [{:keys [buckets capacity]} m
        new-buckets (->> buckets
                         (map (fn [bucket]
                                (->> bucket
                                     (map (fn [[k v]] [k (f v)]))
                                     vec)))
                         vec)]
    {:buckets new-buckets :capacity capacity}))

(defn filter
  [pred m]
  (let [{:keys [buckets]} m
        new-buckets (->> buckets
                         (map (fn [bucket]
                                (->> bucket
                                     (clojure.core/filter (fn [[k v]] (pred k v)))
                                     vec)))
                         vec)]
    (assoc m :buckets new-buckets)))
;; левая сверт
(defn fold
  [f state m]
  (let [{:keys [buckets]} m]
    (reduce (fn [acc bucket]
              (reduce (fn [acc' [k v]] (f acc' k v)) acc bucket))
            state
            buckets)))
;; правая сверт
(defn fold-back
  [f state m]
  (let [{:keys [buckets]} m
        rev-buckets (reverse buckets)]
    (reduce (fn [acc bucket]
              (reduce (fn [acc' [k v]] (f k v acc')) acc (reverse bucket)))
            state
            rev-buckets)))

(defn combine
  [map1 map2]
  (fold (fn [acc k v] (add k v acc)) map2 map1))

(defn equals
  [other m]
  (let [g (fn [s k _] (conj s k))
        s1 (fold g #{} m)
        all-keys (fold g s1 other)]
    (every? (fn [k]
              (= (try-find k m) (try-find k other)))
            all-keys)))
