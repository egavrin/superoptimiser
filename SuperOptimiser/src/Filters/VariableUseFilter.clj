(ns Filters.VariableUseFilter)
(use 'clojure.test)
(use 'Main.Global)

; The VariableUseFilter checks that all accesses to variables occur only once a variable has had a value
; written into it (either at startup, because it corresponds to an argument to the method, or thanks to 
; a later ISTORE operation). It also checks that variables written to are subsequently read, before being
; written to again. Two consecutive writes to a variable mean one such write is redundant.

(defn update-varmap
  "Takes a sequence starting with an opcode and followed by arguments, returns nil or an updated key/value pair for a hash"
  [s]
  (let [op (first s)]
	  (cond
	    (= op :iload_0) '(0 :read)
	    (= op :iload_1) '(1 :read)
	    (= op :iload_2) '(2 :read)
	    (= op :iload_3) '(3 :read)
      (= op :iload) (seq [(nth s 1) :read])
	    (= op :istore_0) '(0 :write)
	    (= op :istore_1) '(1 :write)
	    (= op :istore_2) '(2 :write)
	    (= op :istore_3) '(3 :write)
      (= op :istore) (seq [(nth s 1) :write])
      :else nil)))

(is (= '(0 :write) (update-varmap '[:istore_0])))
(is (= '(3 :read) (update-varmap '[:iload_3])))
(is (= '(7 :write) (update-varmap '[:istore 7])))
(is (= '(12 :read) (update-varmap '[:iload 12])))

(defn uses-vars-ok?
  "Does the supplied sequence try to read from local variables only after they're written to, and not overwrite values in variables?"
  [nv l]
  (let [initial-hash (into {} (map #(assoc {} (identity %) :write) (range 0 nv)))]
    (loop [head l last-op initial-hash]
      (let [op_args (first head) op (first op_args) vm-update (update-varmap op_args)]
	      (cond
	        (empty? head) true
         
         ; If we're reading from, a variable which has never been written, fail the sequence
         
	        (and (= op :iload_0) (= nil (get last-op 0))) false 
	        (and (= op :iload_1) (= nil (get last-op 1))) false
	        (and (= op :iload_2) (= nil (get last-op 2))) false
	        (and (= op :iload_3) (= nil (get last-op 3))) false
          (and (= op :iload) (= nil (get last-op (nth head 1)))) false

         ; handle :iload
         
         ; If we're writing from a variable which we last wrote to (i.e. overwriting data), fail the sequence
	        (and (= op :istore_0) (= :write (get last-op 0))) false
	        (and (= op :istore_1) (= :write (get last-op 1))) false
	        (and (= op :istore_2) (= :write (get last-op 2))) false
	        (and (= op :istore_3) (= :write (get last-op 3))) false
          (and (= op :istore) (= :write (get last-op (nth head 1)))) false
         
         ; otherwise record the read-write state; skip the appropriate number of instructions; carry on
          
	        :else (if (= nil vm-update) (recur (rest head) last-op)
                 (recur (rest head) (assoc last-op (nth vm-update 0) (nth vm-update 1)))))))))

(is (= true (uses-vars-ok? 0 '((:ixor)))))
(is (= false (uses-vars-ok? 0 '((:iload_0)))))
(is (= true (uses-vars-ok? 0 '((:istore_0) (:iload_0)))))
(is (= true (uses-vars-ok? 0 '((:istore_0) (:iload_0)))))
(is (= false (uses-vars-ok? 0 '((:istore_1) (:iload_0)))))
(is (= false (uses-vars-ok? 0 '((:istore_0) (:istore_0)))))
(is (= true (uses-vars-ok? 0 '((:istore_0) (:istore_1)))))
(is (= true (uses-vars-ok? 0 '((:istore_0) (:iload_0) (:istore_0)))))
(is (= true (uses-vars-ok? 0 '((:istore_0) (:iload_0) (:istore_0) (:iload_0) (:iload_0)))))
(is (= false (uses-vars-ok? 0 '((:istore_0) (:iload_1) (:istore_0)))))
(is (= true (uses-vars-ok? 1 '((:iload_0)))))
(is (= false (uses-vars-ok? 1 '((:bipush) (:iload_3) (:ireturn)))))
