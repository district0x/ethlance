(ns ethlance.shared.random)

(def ^:dynamic *dist-resolution* 10000000000)

(defn get-distribution [norm-factor tupl]
  (loop [cstart 0
         tupl tupl
         distrib []]
    (if-let [tup (first tupl)]
      (let [[percent value] tup
            cend (+ cstart (* percent norm-factor))]
        (recur cend
               (rest tupl)
               (conj distrib [[cstart cend] value])))
      distrib)))

(defn -pick-rand-by-dist [ds]
  (let [r (rand *dist-resolution*)]
    (->> ds
         (filter (fn [[[start end] _]]
                   (and (<= start r)
                        (> end r))))
         first
         second)))

(defn pick-rand-by-dist
  "Pick a value from the provided tuple pairs, where the first value
  represents a weight value on being picked, and the second value is
  the item that will be returned upon being selected.

  # Examples

  ;; 50% chance to return :heads or :tails
  (pick-rand-by-dist [[50 :heads] [50 :tails]])

  ;; Has a 25% chance of returning either :head, :body, :arms, or :groin
  (pick-rand-by-dist
   [[1 :head]
    [1 :body]
    [1 :arms]
    [1 :groin]])

  ;; 1 in a million chance of winning the lottery+
  (pick-rand-by-dist
   [[1         :lottery]
    [(dec 1e9) :loss]])
  "
  [tupl]
  (let [total-percent (->> tupl
                           (map first)
                           (reduce +))
        norm-factor (/ *dist-resolution* total-percent)
        distrib (get-distribution norm-factor tupl)]
    (-pick-rand-by-dist distrib)))

(defn pluck!
  "Plucks a random value from an atom containing a sequence, and updates
  the sequence with the plucked value removed. An empty sequence
  returns nil."
  [*coll]
  (assert (sequential? @*coll) "Derefed Atom must be sequential")
  (when-not (empty? @*coll)
    (let [val (rand-nth @*coll)]
      (swap! *coll (fn [v] (->> v (remove #(= val %)) (into (empty @*coll)))))
      val)))

(defn rand-nth-n
  "Retrieve `n` random distinct values from the collection `coll` and return it as a sequence.

  Notes:

  - If `n` exceeds the count of `coll`, the function returns early
  if (count coll) elements.
  "
  [coll n]
  (assert (sequential? coll) "Provided collection must be sequential")
  (let [*coll (atom coll)]
    (loop [i 0 result (list)]
      (if (< i n)
        (if-let [val (pluck! *coll)]
          (recur (inc i) (cons val result))
          result)
        result))))
