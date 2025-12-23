(ns sc-set
  (:refer-clojure :exclude [empty remove filter map]))

(def ^:private default-capacity 16)
;; Коэффициент загрузки (load factor) определяет, при каком проценте заполнения
;; необходимо увеличивать размер хеш-таблицы. Значение 0.75 - стандартное для
;; большинства реализаций хеш-таблиц (Java HashMap, Python dict и т.д.).
;; Это означает, что когда количество элементов достигает 75% от capacity,
;; происходит resize для поддержания эффективности операций.
(def ^:private load-factor 0.75)

(defn- new-buckets
  [capacity]
  (vec (repeat capacity [])))

(defn empty
  "Создаёт пустое множество.

   ИЗМЕНЕНИЕ: Добавлено поле :size для отслеживания количества элементов.
   В старой версии:
   (defn empty []
     {:buckets (vec (repeat default-capacity []))
      :capacity default-capacity})

   Поле :size необходимо для:
   1. Эффективной проверки необходимости resize (без подсчёта элементов)
   2. Оптимизации функции equals (быстрая проверка размера)
   3. Публичного API через функцию size"
  []
  {:buckets  (new-buckets default-capacity)
   :capacity default-capacity
   :size     0})

(defn- bucket-index
  [capacity e]
  (mod (hash e) capacity))

(defn- threshold
  "Вычисляет пороговое значение количества элементов, при достижении которого
   необходимо выполнить resize хеш-таблицы.

   НОВАЯ ФУНКЦИЯ: В старой версии отсутствовала.

   Примеры:
   - capacity = 16, load-factor = 0.75 → threshold = 12
   - capacity = 32, load-factor = 0.75 → threshold = 24

   Когда size >= threshold, происходит увеличение capacity в 2 раза и rehash.
   Это гарантирует, что средняя длина цепочек в buckets остаётся небольшой,
   обеспечивая эффективность операций поиска и вставки (O(1) в среднем)."
  [capacity]
  (max 1 (int (Math/ceil (* capacity load-factor)))))

(defn- rehash
  "Перестраивает хеш-таблицу с новым размером capacity.

   НОВАЯ ФУНКЦИЯ: В старой версии отсутствовала.

   ПРОБЛЕМА БЕЗ RESIZE:
   В старой версии capacity был фиксированным (16). При добавлении большого
   количества элементов все они попадали в одни и те же 16 buckets, создавая
   длинные цепочки. Это приводило к деградации производительности:
   - Поиск: O(1) → O(n) в худшем случае
   - Вставка: O(1) → O(n) в худшем случае

   РЕШЕНИЕ:
   При увеличении capacity элементы перераспределяются по новым buckets
   согласно их хеш-значениям. Это уменьшает длину цепочек и восстанавливает
   эффективность операций.

   АЛГОРИТМ:
   1. Создаём новый вектор buckets размером new-capacity
   2. Проходим по всем старым buckets
   3. Для каждого элемента вычисляем новый индекс через bucket-index
   4. Размещаем элемент в соответствующем новом bucket

   ПРИМЕР:
   Старая таблица: capacity=16, элемент с hash=25 → bucket 9 (25 mod 16)
   Новая таблица: capacity=32, тот же элемент → bucket 25 (25 mod 32)
   Элементы распределяются более равномерно!"
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
  "Проверяет необходимость увеличения размера хеш-таблицы и выполняет resize
   при необходимости.

   НОВАЯ ФУНКЦИЯ: В старой версии отсутствовала.

   КОГДА ВЫЗЫВАЕТСЯ:
   После каждого добавления элемента (в функции add).

   УСЛОВИЕ RESIZE:
   size >= threshold(capacity), где threshold = capacity * load-factor

   ЧТО ПРОИСХОДИТ:
   - capacity увеличивается в 2 раза (16 → 32 → 64 → 128 ...)
   - Вызывается rehash для перераспределения всех элементов

   ПОЧЕМУ УВЕЛИЧИВАЕМ В 2 РАЗА:
   - Простота реализации (битовый сдвиг)
   - Гарантирует, что после resize load factor ≈ 0.375 (половина от 0.75)
   - Это даёт запас до следующего resize

   ПРОИЗВОДИТЕЛЬНОСТЬ:
   - Resize происходит редко (логарифмически по отношению к количеству элементов)
   - Амортизированная сложность операций остаётся O(1)"
  [s]
  (let [{:keys [size capacity]} s]
    (if (>= size (threshold capacity))
      (rehash s (* 2 capacity))
      s)))

(defn- without-elem
  [e bucket]
  (vec (clojure.core/remove #(= % e) bucket)))

(defn add
  "Добавляет элемент в множество.

   ИЗМЕНЕНИЯ для поддержки resize:

   СТАРАЯ ВЕРСИЯ:
   (defn add [e s]
     (update-bucket s e
                    (fn [bucket]
                      (conj (without-elem e bucket) e))))

   НОВАЯ ВЕРСИЯ:
   1. Добавлено отслеживание :size - инкрементируется при добавлении нового элемента
   2. Добавлен вызов ensure-capacity после добавления

   КАК РАБОТАЕТ RESIZE:
   После добавления элемента проверяется условие:
   - Если size >= threshold(capacity) → вызывается ensure-capacity
   - ensure-capacity вызывает rehash с capacity * 2
   - Все элементы перераспределяются по новым buckets
   - Структура возвращается с обновлёнными :buckets и :capacity

   ПРИМЕР РАБОТЫ:
   Начальное состояние: capacity=16, size=0, threshold=12
   Добавляем 12 элементов: capacity=16, size=12, threshold=12
   Добавляем 13-й элемент: size=13 >= threshold=12 → RESIZE!
   После resize: capacity=32, size=13, threshold=24

   БЕЗ RESIZE (старая версия):
   При 100 элементах все попадают в 16 buckets → средняя длина цепочки = 6.25
   Поиск элемента: нужно проверить ~6 элементов в среднем

   С RESIZE (новая версия):
   При 100 элементах capacity=128, threshold=96 → средняя длина цепочки ≈ 0.78
   Поиск элемента: нужно проверить ~1 элемент в среднем"
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
  "Удаляет элемент из множества.

   ИЗМЕНЕНИЕ: Добавлено отслеживание :size - декрементируется при удалении элемента.

   В старой версии поле :size отсутствовало, поэтому размер не отслеживался.
   Теперь при успешном удалении size уменьшается на 1.

   ПРИМЕЧАНИЕ: Resize при удалении не выполняется (только при добавлении).
   Это стандартное поведение для большинства реализаций хеш-таблиц."
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
    (letfn [(check [elems buckets]
              (cond
                (seq elems) (and (contains-element (first elems) b)
                                 (recur (rest elems) buckets))
                (seq buckets) (recur (first buckets) (rest buckets))
                :else true))]
      (check [] (:buckets a)))))
