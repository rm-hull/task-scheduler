(ns task-scheduler.core
  "An abstraction over Java's Fork/Join mechanism: A Fork/Join task is a
  thread-like entity that is much lighter weight than a normal thread.
  Huge numbers of tasks and subtasks may be hosted by a small number of
  actual threads in a ForkJoinPool, at the price of some usage limitations."
  (:import
    [java.util.concurrent ForkJoinPool ForkJoinWorkerThread ForkJoinTask RecursiveTask]))

(def ^:private ^ForkJoinPool pool (ForkJoinPool/commonPool))

(defn submit
  "Checks the current thread before submitting the task to the Fork/Join
  pool. Note: it is not necessary to call this function directly, as `fork`
  will automatically submit onto the fork/join pool."
  [^ForkJoinTask task]
  (if (ForkJoinTask/inForkJoinPool)
    (.fork task)
    (.execute pool task))
  task)

(defmacro fork
  "Constructs a `RecursiveTask` proxy object who's abstract compute()
  method is fulfilled by the supplied body. The task is then submitted
  for execution onto a Fork/Join pool.

  Computations should ideally avoid synchronized methods or blocks, and
  should minimize other blocking synchronization apart from joining other
  tasks or using synchronizers such as Phasers that are advertised to
  cooperate with fork/join scheduling. Subdividable tasks should also not
  perform blocking I/O, and should ideally access variables that are
  completely independent of those accessed by other running tasks. These
  guidelines are loosely enforced by not permitting checked exceptions
  such as IOExceptions to be thrown. However, computations may still
  encounter unchecked exceptions, that are rethrown to callers attempting
  to join them. [JDK8 docs on ForkJoinTask]"
  [& body]
  `(submit
    (proxy [RecursiveTask] []
      (compute ([] (do ~@body))))))

(defn join
  "Returns the result of the computation when it is done. Abnormal
  completion results in RuntimeException or Error."
  [^ForkJoinTask task]
  (.join task))
