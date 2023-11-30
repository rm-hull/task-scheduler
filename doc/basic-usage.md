# Basic Usage

The first step for using the fork/join task-scheduler framework is to write
code that performs a segment of the work. Your code should look similar to the
following pseudocode:

```
if (my portion of the work is small enough)
  do the work directly
else
  split my work into two pieces
  invoke the two pieces and wait for the results
```

Wrap this code in a `fork` block, which will typically return a [RecursiveTask](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/RecursiveTask.html),
having submitted it to a ForkJoinPool instance.

### A dumb way to compute Fibonacci numbers

As a simple example of using the fork/join mechanism, what follows is a naïve
Fibonacci implementation to illustrate library use (which was more-or-less
lifted straight from the java version in JDK8 docs), rather than an efficient
algorithm.

So, starting with a textbook recursive Fibonacci function:

```clojure
(defn fib [n]
  (if (<= n 1)
    n
    (let [f1 (fib (- n 1))
          f2 (fib (- n 2))]

      (+ f1 f2))))

(map fib (range 1 20)))
; => (1 1 2 3 5 8 13 21 34 55 89 144 233 377 610 987 1597 2584 4181)
```

The important thing to note here is that the calculation is split into two
sub-calls (to `(fib (- n 1))` and `(fib (- n 2))`) which are performed
recursively until _n_ reaches 1. Large values of _n_ result in a combinatorial
explosion, but that is not of particular concern here.

Referring to the pseudocode above, the only modification we need to in order to
make use of the fork/join task scheduler is to wrap the sub-calls, and then
wait for the results. Compare the fork/join version:

```clojure
(use 'task-scheduler.core)

(defn fib [n]
  (if (<= n 1)
    n
    (let [f1 (fork (fib (- n 1)))
          f2 (fork (fib (- n 2)))]

      (+ (join f1) (join f2)))))

(map fib (range 1 20)))
; => (1 1 2 3 5 8 13 21 34 55 89 144 233 377 610 987 1597 2584 4181)
```

The `fork` creates a new task for each sub-call (which would be executed in
parallel), while the result is returned out of the `join`. The structure and
flow of the two implementations is exactly the same.

Note this particular implementation is likely to perform poorly because the
smallest subtasks are too small to be worthwhile splitting up. Instead, as is
the case for nearly all fork/join applications, you'd pick some minimum
granularity size (for example 10 here) for which you always sequentially solve
rather than subdividing.

#### Bootnote

Typically, a fast linear algorithm for calculating Fibonacci numbers might be
along the lines of:

```clojure
(defn fib [a b]
  (cons a (lazy-seq (fib b (+ a b)))))

(take 10 (fib 1 1))
; => (1 1 2 3 5 8 13 21 34 55)
```

### Parallel Sum

Another example (this time from [Dan Grossman](http://homes.cs.washington.edu/~djg/)'s _Parallelism and
Concurrency_ course), converted from Java into Clojure, to sum up
10-million integers:

```clojure
(def ^:dynamic *sequential-threshold* 5000)

(defn sum
  ([arr]
   (sum arr 0 (count arr)))

  ([arr lo hi]
   (if (<= (- hi lo) *sequential-threshold*)
     (reduce + (subvec arr lo hi))
     (let [mid (+ lo (quot (- hi lo) 2))
           left (fork (sum arr lo mid))
           right (fork (sum arr mid hi))]
       (+ (join left) (join right))))))

(def arr (vec (range 100000000)))

(time (reduce + arr))
;=> "Elapsed time: 317.234628 msecs"
;=> 49999995000000

(time (sum arr))
;=> "Elapsed time: 186.297616 msecs"
;=> 49999995000000
```

For comparison, the parallel sum implementation is approximately twice as
fast as `(reduce + arr)`, as measured on an Intel Core i5-5257U CPU @ 2.70GHz.

Setting `*sequential-threshold*` to a good-in-practice value is a trade-off.
The documentation for the Fork/Join framework suggests creating parallel
subtasks until the number of basic computation steps is somewhere over 100 and
less than 10,000. The exact number is not crucial provided you avoid extremes.

#### Can we do better?

Look carefully at the implementation of parallel sum: `left` and `right` are
forked tasks, while the current thread is blocked waiting for them both to
yield their results.

The revised version below still forks the `left` value, but `right` value is
computed in-line, thereby eliminating creation of more parallel tasks than is
necessary; this is _slightly_ more efficient at the expense of seeming somewhat
asymmetrical.

```clojure
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
```

However, the order is crucial, if the `left` had been joined before `right`
invoked, or `right` computed before `left` forked, then the entire
array-summing algorithm would have no parallelism at all since each step would
compute sequentially.

This may've been important for JSR166/JDK6 & JDK7, but I believe that this is
no longer the case for JDK8, so for the sake of avoiding the left/right
ordering 'gotcha', use fork in all cases.

### Implicit Equations

Any computation that is [embarrassingly parallel](https://en.wikipedia.org/wiki/Embarrassingly_parallel) can make use of the fork/join
mechanism to split the work effort into disparate chunks. For example, the
[implicit-equations](https://github.com/rm-hull/implicit-equations) project takes an equation of the form:

```clojure
(defn dizzy [x y]
  (infix abs(sin(x ** 2 - y ** 2)) - (sin(x + y) + cos(x . y))))
```

and attempts to plot Cartesian coordinates where the value crosses zero, which
results in some spectacular charts:

![PNG](https://rawgithub.com/rm-hull/implicit-equations/main/doc/dizzy.png)

There is no dependency, or need for communication (or synchronization needed) to
calculate the value for each point, and there is very little effort
required to separate the problem into a number of parallel tasks. The tasks
are split by bands, as illustrated [in the code](https://github.com/rm-hull/implicit-equations/blob/main/src/implicit_equations/plot.clj#L103-L104) and are joined before the function returns.
