(ns task-scheduler.core-test
  (:require
   [clojure.test :refer :all]
   [task-scheduler.core :refer :all]))

(deftest check-single-task
  (is (= 16 (join
             (fork
              (Thread/sleep 100)
              (+ 9 7))))))

(deftest check-recursive-task
  (is (= 16 (join
             (fork
              (let [t2 (fork
                        (Thread/sleep 200)
                        (+ 3 9))]
                (Thread/sleep 100)
                (+ (join t2) 4)))))))

(deftest check-multiple-tasks
  (let [make-task (fn [x] (fork
                           (Thread/sleep (* x 10))
                           (* x x)))
        tasks (map make-task (range 50))]
    (is (= 40425 (reduce + (map join tasks))))))
