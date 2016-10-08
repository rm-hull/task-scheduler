(ns task-scheduler.sum-test
  (:require
   [clojure.test :refer :all]
   [task-scheduler.core :refer :all]))

(def ^:dynamic *sequential-threshold* 5000)

(defn sum
  ([arr]
   (sum arr 0 (count arr)))

  ([arr lo hi]
   (if (<= (- hi lo) *sequential-threshold*)
     (reduce + (subvec arr lo hi))
     (let [mid (+ lo (quot (- hi lo) 2))
           left (fork (sum arr lo mid))
           right (sum arr mid hi)]
       (+ (join left) right)))))

(deftest check-sum
  (let [arr (vec (range 10000000))]
    (is (= (reduce + arr)
           (sum arr)))))
