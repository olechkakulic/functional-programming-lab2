(ns property-tests

  (:require [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]
            [sc-set :as set]))

(def elem-gen gen/small-integer)
(def elems-gen (gen/vector elem-gen))

(defn build
  "Вспомогательная функция для построения множества из списка элементов.
   Используется внутри генераторов."
  [elems]
  (reduce (fn [acc e] (set/add e acc))
          (set/empty)
          elems))

;; НОВЫЕ ГЕНЕРАТОРЫ ДЛЯ СТРУКТУРЫ ДАННЫХ
;;
;; ПРОБЛЕМА В СТАРОЙ ВЕРСИИ:
;; Генераторы создавали только списки элементов (elems-gen), а не готовые
;; экземпляры структуры данных. В тестах приходилось вручную вызывать build:
;;
;; (prop/for-all [elems elems-gen ...]  ; ❌ генерирует список
;;               (let [s (build elems)   ; ❌ вручную строим set
;;                     ...]))
;;
;; Это не соответствует идиоматичному использованию test.check, где генераторы
;; должны создавать объекты того типа, который тестируется.

(def set-gen
  "Генератор для структуры данных sc-set.

   НОВАЯ ФУНКЦИЯ: В старой версии отсутствовала.

   Как работает:
   - gen/fmap применяет функцию build к каждому сгенерированному списку элементов
   - Результат: генератор, который создаёт готовые экземпляры sc-set

   Пример использования:
   (prop/for-all [s set-gen ...]  ; ✅ генерирует готовый set
                 (let [s2 (set/add x s)  ; ✅ сразу работаем со структурой
                       ...]))

   Преимущества:
   1. Тесты проверяют свойства структуры напрямую
   2. Соответствует best practices property-based testing
   3. Код тестов становится чище и понятнее"
  (gen/fmap build elems-gen))

(def non-empty-set-gen
  "Генератор для непустых множеств sc-set.

   НОВАЯ ФУНКЦИЯ: В старой версии отсутствовала.

   Используется в случаях, когда нужно гарантировать непустое множество
   (например, для тестирования операций, которые требуют наличия элементов).

   Как работает:
   - gen/not-empty гарантирует, что список элементов не пустой
   - Затем применяется build для создания непустого множества"
  (gen/fmap build (gen/not-empty elems-gen)))
;; prop-add-then-contains: Проверяет, что после добавления элемента он обязательно содержится в множестве.
;;
;; ИЗМЕНЕНИЕ в использовании генераторов:
;;
;; СТАРАЯ ВЕРСИЯ:
;; (prop/for-all [elems elems-gen    ; ❌ генерирует список элементов
;;                x elem-gen]
;;               (let [s  (build elems)  ; ❌ вручную строим set из списка
;;                     s2 (set/add x s)]
;;                 (set/contains-element x s2)))
;;
;; НОВАЯ ВЕРСИЯ:
;; (prop/for-all [s set-gen          ; ✅ генерирует готовую структуру sc-set
;;                x elem-gen]
;;               (let [s2 (set/add x s)]  ; ✅ сразу работаем со структурой
;;                 (set/contains-element x s2)))
;;
;; ПРЕИМУЩЕСТВА:
;; - Генератор создаёт объекты того типа, который тестируется
;; - Код теста проще и понятнее
;; - Соответствует идиоматичному использованию test.check
(defspec prop-add-then-contains 200
  (prop/for-all [s set-gen
                 x elem-gen]
                (let [s2 (set/add x s)]
                  (set/contains-element x s2))))

;; prop-remove-behaviour: Проверяет поведение удаления: удалённый элемент отсутствует,
;; остальные элементы сохраняют своё состояние.
;;
;; ИЗМЕНЕНИЕ в использовании генераторов:
;;
;; СТАРАЯ ВЕРСИЯ:
;; (prop/for-all [elems elems-gen    ; ❌ генерирует список
;;                x elem-gen]
;;               (let [s         (build elems)  ; ❌ вручную строим set
;;                     s-added   (set/add x s)
;;                     s-removed (set/remove x s-added)]
;;                 (and (not (set/contains-element x s-removed))
;;                      (every? (fn [y] ...) elems))))  ; ❌ проверяем исходный список
;;
;; НОВАЯ ВЕРСИЯ:
;; (prop/for-all [s set-gen          ; ✅ генерирует готовую структуру
;;                x elem-gen]
;;               (let [s-added   (set/add x s)
;;                     s-removed (set/remove x s-added)
;;                     all-elems (set/fold ... s)]  ; ✅ получаем элементы через API
;;                 (and (not (set/contains-element x s-removed))
;;                      (every? (fn [y] ...) all-elems))))
;;
;; УЛУЧШЕНИЕ:
;; - Используем set/fold для получения элементов через публичное API
;; - Это гарантирует, что тест работает только с публичным интерфейсом
;; - Не зависит от внутреннего представления структуры
(defspec prop-remove-behaviour 200
  (prop/for-all [s set-gen
                 x elem-gen]
                (let [s-added   (set/add x s)
                      s-removed (set/remove x s-added)
                      all-elems (set/fold (fn [acc e] (conj acc e)) [] s)]
                  (and (not (set/contains-element x s-removed))
                       (every? (fn [y]
                                 (if (= y x)
                                   true
                                   (= (set/contains-element y s)
                                      (set/contains-element y s-removed))))
                               all-elems)))))

;; prop-monoid-identity: Проверяет свойство нейтрального элемента моноида:
;; combine(empty, s) = s и combine(s, empty) = s
;;
;; ИЗМЕНЕНИЕ в использовании генераторов:
;;
;; СТАРАЯ ВЕРСИЯ:
;; (prop/for-all [elems elems-gen]  ; ❌ генерирует список
;;               (let [s (build elems)  ; ❌ вручную строим set
;;                     e (set/empty)]
;;                 ...))
;;
;; НОВАЯ ВЕРСИЯ:
;; (prop/for-all [s set-gen]  ; ✅ генерирует готовую структуру
;;               (let [e (set/empty)]
;;                 ...))
;;
;; ПРЕИМУЩЕСТВО:
;; - Тест напрямую проверяет свойство структуры данных
;; - Не нужно промежуточное преобразование списка в set
(defspec prop-monoid-identity 200
  (prop/for-all [s set-gen]
                (let [e (set/empty)]
                  (and (set/equals s (set/combine e s))
                       (set/equals s (set/combine s e))))))

;; prop-monoid-associative: Проверяет ассоциативность операции combine:
;; combine(a, combine(b, c)) = combine(combine(a, b), c)
;;
;; ИЗМЕНЕНИЕ в использовании генераторов:
;;
;; СТАРАЯ ВЕРСИЯ:
;; (prop/for-all [ea elems-gen    ; ❌ генерируем 3 списка
;;                eb elems-gen
;;                ec elems-gen]
;;               (let [a (build ea)  ; ❌ вручную строим 3 множества
;;                     b (build eb)
;;                     c (build ec)
;;                     ...))
;;
;; НОВАЯ ВЕРСИЯ:
;; (prop/for-all [a set-gen  ; ✅ генерируем 3 готовых множества
;;                b set-gen
;;                c set-gen]
;;               (let [left  (set/combine a (set/combine b c))
;;                     right (set/combine (set/combine a b) c)]
;;                 ...))
;;
;; ПРЕИМУЩЕСТВА:
;; - Код намного чище и понятнее
;; - Генераторы создают объекты нужного типа напрямую
;; - Соответствует принципам property-based testing
(defspec prop-monoid-associative 200
  (prop/for-all [a set-gen
                 b set-gen
                 c set-gen]
                (let [left  (set/combine a (set/combine b c))
                      right (set/combine (set/combine a b) c)]
                  (set/equals left right))))
(def funcs
  (gen/elements [inc dec (fn [x] (* 2 x)) identity]))

;; prop-map-composition: Проверяет свойство композиции map:
;; map(f, map(g, s)) = map(comp(f, g), s)
;;
;; ИЗМЕНЕНИЕ в использовании генераторов:
;;
;; СТАРАЯ ВЕРСИЯ:
;; (prop/for-all [elems elems-gen    ; ❌ генерирует список
;;                f funcs
;;                g funcs]
;;               (let [s     (build elems)  ; ❌ вручную строим set
;;                     left  (set/map f (set/map g s))
;;                     right (set/map (comp f g) s)]
;;                 ...))
;;
;; НОВАЯ ВЕРСИЯ:
;; (prop/for-all [s set-gen  ; ✅ генерирует готовую структуру
;;                f funcs
;;                g funcs]
;;               (let [left  (set/map f (set/map g s))
;;                     right (set/map (comp f g) s)]
;;                 ...))
;;
;; ПРЕИМУЩЕСТВО:
;; - Генератор создаёт объект того типа, который тестируется
;; - Тест фокусируется на проверке свойства, а не на построении структуры
(defspec prop-map-composition 200
  (prop/for-all [s set-gen
                 f funcs
                 g funcs]
                (let [left  (set/map f (set/map g s))
                      right (set/map (comp f g) s)]
                  (set/equals left right))))
