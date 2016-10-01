(ns task-scheduler.fibonacci-test
  (:require
    [clojure.test :refer :all]
    [task-scheduler.core :refer :all]))

(defn fib [n]
  (if (<= n 1)
    n
    (let [f1 (fork (fib (- n 1)))
          f2 (fork (fib (- n 2)))]

      (+ (join f1) (join f2)))))

(deftest check-fork-join
  (is (= (list 0 1 1 2 3 5 8 13 21 34)
         (map fib (range 10)))))
