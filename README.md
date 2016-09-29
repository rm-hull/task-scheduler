# Task Scheduler
[![Build Status](https://travis-ci.org/rm-hull/task-scheduler.svg?branch=master)](http://travis-ci.org/rm-hull/task-scheduler) [![Coverage Status](https://coveralls.io/repos/rm-hull/task-scheduler/badge.svg?branch=master)](https://coveralls.io/r/rm-hull/task-scheduler?branch=master) [![Dependencies Status](https://jarkeeper.com/rm-hull/task-scheduler/status.svg)](https://jarkeeper.com/rm-hull/task-scheduler) [![Downloads](https://jarkeeper.com/rm-hull/task-scheduler/downloads.svg)](https://jarkeeper.com/rm-hull/task-scheduler) [![Clojars Project](https://img.shields.io/clojars/v/rm-hull/task-scheduler.svg)](https://clojars.org/rm-hull/task-scheduler)

Task scheduler is a library designed to abstract over Java's [Fork/Join](https://docs.oracle.com/javase/tutorial/essential/concurrency/forkjoin.html)
framework, in order to help make using lightweight fork/join tasks more idiomatic in Clojure.
Fork/Join was designed for tasks that can be recursively broken down into smaller
pieces. The inspiration and motivation for this library was the excellent Coursera
[Parallel Programming in Scala](https://www.coursera.org/learn/parprog1/home/welcome) course run by _École Polytechnique Fédérale de Lausanne_.

Mostly, the implementation consists of wrapping the Java implementation, but
crucially includes a `task` macro which accepts a body, converts this into a
recursive task which is then automatically submitted to the fork/join executor,
to be run when there is available capacity. The result is available from the
blocking `join` function.

### Pre-requisites

You will need [Leiningen](https://github.com/technomancy/leiningen) 2.6.1 or above installed.

### Building

To build and install the library locally, run:

    $ cd task-scheduler
    $ lein test
    $ lein install

### Including in your project

There is a version hosted at [Clojars](https://clojars.org/rm-hull/task-scheduler).
For leiningen include a dependency:

```clojure
[rm-hull/task-scheduler "0.1.0"]
```

For maven-based projects, add the following to your `pom.xml`:

```xml
<dependency>
  <groupId>rm-hull</groupId>
  <artifactId>task-scheduler</artifactId>
  <version>0.1.0</version>
</dependency>
```

## API Documentation

See [www.destructuring-bind.org/task-scheduler](http://www.destructuring-bind.org/task-scheduler/) for API details.

## Basic Usage

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

Wrap this code in a `task` block, which will typically return a [RecursiveTask](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/RecursiveTask.html),
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
(defn fib [n]
  (if (<= n 1)
    n
    (let [f1 (task (fib (- n 1)))
          f2 (task (fib (- n 2)))]

      (+ (join f1) (join f2)))))

(map fib (range 1 20)))
; => (1 1 2 3 5 8 13 21 34 55 89 144 233 377 610 987 1597 2584 4181)
```

The `task` forks each sub-call in a different thread, and only returns the
result out of the `join`. The structure and flow of the two implementations
is exactly the same.

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

![PNG](https://rawgithub.com/rm-hull/implicit-equations/master/doc/dizzy.png)

There is no dependency, or need for communication (or synchronization needed) to
calculate the value for each point, and there is very little effort
required to separate the problem into a number of parallel tasks. The tasks
are split by bands, as illustrated [in the code](https://github.com/rm-hull/implicit-equations/blob/master/src/implicit_equations/plot.clj#L103-L104) and are joined before the function returns.

## References

* https://www.coursera.org/learn/parprog1/home/welcome
* https://docs.oracle.com/javase/tutorial/essential/concurrency/forkjoin.html
* https://github.com/rm-hull/implicit-equations
* http://homes.cs.washington.edu/~djg/teachingMaterials/grossmanSPAC_forkJoinFramework.html#useful

## License

The MIT License (MIT)

Copyright (c) 2016 Richard Hull

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
Fork/Join task scheduling in Clojure