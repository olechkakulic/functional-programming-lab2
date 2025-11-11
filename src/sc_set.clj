(ns sc-set
  (:refer-clojure :exclude [filter]))
(defn empty
  "Создать пустую HashMap (capacity = 16)."
  []
  {:buckets (vec (repeat 16 []))
   :capacity 16})

(defn- bucket-index
  "Вычисление индекса бакета по key и capacity."
  [capacity key]
  (mod (hash key) capacity))

(defn add
  "Добавление пары (key value). Если ключ уже есть — значение заменяется.
   Возвращает новую HashMap."
  [key value m]
  (let [{:keys [buckets capacity]} m
        idx (bucket-index capacity key)
        bucket (nth buckets idx)
        ;; создаём новый вектор filtered, который содержит все пары из bucket, кроме тех, у которых ключ равен key.
        filtered (->> bucket (remove (fn [[k _]] (= k key))) vec)
        updated-bucket (into [[key value]] filtered)
        new-buckets (assoc buckets idx updated-bucket)]
    (assoc m :buckets new-buckets)))

(defn remove
  "Удаление элемента по ключу. Возвращает новую HashMap."
  [key m]
  (let [{:keys [buckets capacity]} m
        idx (bucket-index capacity key)
        updated-bucket (->> (nth buckets idx) (clojure.core/remove (fn [[k _]] (= k key))) vec)
        new-buckets (assoc buckets idx updated-bucket)]
    (assoc m :buckets new-buckets)))

(defn try-find
  "Поиск значения по ключу. Возвращает значение или nil, если не найдено."
  [key m]
  (let [{:keys [buckets capacity]} m
        idx (bucket-index capacity key)]
    (some (fn [[k v]] (when (= k key) v)) (nth buckets idx))))

(defn contains-key
  "Проверка наличия ключа (true/false)."
  [key m]
  (boolean (try-find key m)))

(defn map-values
  "Применяет функцию f ко всем значениям, возвращает новую HashMap<'K,'U>."
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
  "Фильтрация по предикату pred: (pred key value) -> boolean.
   Возвращает новую HashMap с оставшимися парами."
  [pred m]
  (let [{:keys [buckets capacity]} m
        new-buckets (->> buckets
                         (map (fn [bucket]
                                (->> bucket
                                     (clojure.core/filter (fn [[k v]] (pred k v)))
                                     vec)))
                         vec)]
    (assoc m :buckets new-buckets)))

(defn fold
  "Левая свёртка. f — функция (acc key value) -> acc.
   fold f state map"
  [f state m]
  (let [{:keys [buckets]} m]
    (reduce (fn [acc bucket]
              (reduce (fn [acc' [k v]] (f acc' k v)) acc bucket))
            state
            buckets)))

(defn fold-back
  "Правая свёртка. f — функция (key value acc) -> acc.
   foldBack f state map"
  [f state m]
  (let [{:keys [buckets]} m
        rev-buckets (reverse buckets)]
    (reduce (fn [acc bucket]
              (reduce (fn [acc' [k v]] (f k v acc')) acc (reverse bucket)))
            state
            rev-buckets)))

(defn combine
  "Объединение двух HashMap.
   Логика сохранена с оригинала — fold (fn [acc k v] (add k v acc)) map2 map1"
  [map1 map2]
  (fold (fn [acc k v] (add k v acc)) map2 map1))

(defn equals
  "Сравнение двух HashMap по значениям (без сортировки).
   Сигнатура: (equals other map) — pipe-friendly аналог F#."
  [other m]
  (let [g (fn [s k _] (conj s k))
        s1 (fold g #{} m)
        all-keys (fold g s1 other)]
    (every? (fn [k]
              (= (try-find k m) (try-find k other)))
            all-keys)))
