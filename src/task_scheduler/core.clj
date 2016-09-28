(ns task-scheduler.core
  "An abstraction over Java's Fork/Join mechanism"
  (:import
    [java.util.concurrent ForkJoinPool ForkJoinWorkerThread ForkJoinTask RecursiveTask]))

(def ^:private ^ForkJoinPool pool (ForkJoinPool.))

(defn submit [^ForkJoinTask task]
  (if (instance? ForkJoinWorkerThread (Thread/currentThread))
    (.fork task)
    (.execute pool task))
  task)

(defmacro task [& body]
  `(submit
    (proxy [RecursiveTask] []
      (compute ([] (do ~@body))))))

(defn join [^ForkJoinTask task]
  (.join task))
