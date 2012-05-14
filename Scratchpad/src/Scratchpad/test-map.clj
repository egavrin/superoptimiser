(ns MessingAbout.test-map )
(use 'clojure.test)
(use '[clojure.data.priority-map])

; Test Maps are a concept I use in a couple of places in the SO. They implement a simple
; statistical model: a record is kept of test failures, and tests are run in descending order
; of how often they have previously failed. In this way, I hope to minimise the number of tests
; which must be run.
;
; A Test Map is used in two places:
; 1. To strip out obviously bad bytecode sequences before they are ever converted into a class,
; loaded and run. For instance, any sequence which never returns is obviously bad.
; 2. To order the running of equivalence tests on candidate classes
;

; These two trivial functions are used for our unit tests

(defn no-As?
  "Predicate that returns true if the supplied sequence doesn't include A"
  [l]
  (not (some #(= \A %) l))
)

(defn no-Bs?
  "Predicate that returns true if the supplied sequence doesn't include B"
  [l]
  (not (some #(= \B %) l))
)

; As we use Test Maps in a couple of places, each one must keep track of its own list of
; test functions. When a Test Map is first initialised, we build a map from the list of 
; functions passed in, and initialise the failure count for each function to 0.

(defn test-map
  "Instantiates a new test-map; pass in a sequence of test functions"
  [flist]
  (let [m (priority-map {})]
    (atom (into m (map #(vector % 0) flist)))))

(defn inc-fail-count
  "Increments the fail count for function f in test map tm"
  [tm f]
  (swap! tm conj [f (+ (@tm f) 1)])
)

; unit tests for inc-fail-count

(let [tm (test-map [no-As? no-Bs?]) a-test (key (first @tm)) b-test (key (last @tm))]
  
  ; Increment a-test and check that the values are correct
  
  (inc-fail-count tm a-test)
  (is (= 1 (@tm a-test)))
  (is (= 0 (@tm b-test)))
  (inc-fail-count tm a-test)
  (is (= 2 (@tm a-test)))
  (is (= 0 (@tm b-test)))
  
  ; Check the ordering is b-test then a-test
  
  (is (= b-test (key (first @tm))))
  
  ; Increment b-test until it's larger than a-test; check values and ordering
  
  (inc-fail-count tm b-test)
  (inc-fail-count tm b-test)
  (inc-fail-count tm b-test)
  (is (= 2 (@tm a-test)))
  (is (= 3 (@tm b-test)))
  
  (is (= a-test (key (first @tm)))))


;TODO this should increment the fail count for the first failure
(defn passes?
  "Returns true if the sequence provided passes all the tests in the test map provided"
  [tm s]
  (every? true? ((apply juxt (reverse (keys @tm))) s)))

; unit tests for passes? function

(let [t (test-map [no-As? no-Bs?])] 
  (is (= true (passes? t '[\E \F \G])))
  (is (= true (passes? t '[])))

	(is (= false (passes? t '[\A \B \C \D \E])))
	(is (= false (passes? t '[\A])))
	(is (= false (passes? t '[\B])))
	(is (= false (passes? t '[\C \D \A \E])))
	(is (= false (passes? t '[\C \D \E \B]))))


; we only want those sample predicates for unit tests, so clear them out of our namespace
(ns-unmap 'MessingAbout.test-map 'no-As?)
(ns-unmap 'MessingAbout.test-map 'no-Bs?)
