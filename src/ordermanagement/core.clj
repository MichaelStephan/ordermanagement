(ns ordermanagement.core)

(defn create-order []
  {:id (gensym)
   :line-items []})

(defn merge-line-items [{:keys [line-items] :as order}]
  (assoc order :line-items
         (into []
               (for [[k v] (group-by (fn [{:keys [product-ref rate-plan-ref]}] (str product-ref rate-plan-ref)) line-items)]
                 (reduce (fn [ret line-item]
                           (merge ret line-item {:quantity ((fnil + 0 0) (:quantity ret) (:quantity line-item))})) {}  v)))))

(defn create-line-item [{:keys [line-items] :as order} product-ref rate-plan-ref quantity config]
  (let [line-item {:product-ref product-ref
                   :rate-plan-ref rate-plan-ref
                   :quantity quantity
                   :config config}]
    (-> order
        (update :line-items conj line-item)
        (merge-line-items))))

(defn remove-line-item [{:keys [line-items] :as order} idx]
  (assoc order :line-items (keep-indexed #(when-not (= %1 idx) %2) line-items)))

(defn estimate-line-item-net-price [line-item]
  (let [plan (line-item :rate-plan-ref)]
    (* (plan :price) (line-item :quantity))))

(defn estimate-order-net-price [order]
  (let [line-items (order :line-items)]
      (reduce + (map estimate-line-item-net-price line-items))))

(defn checkout [order]
  (println "Make user pay $" (estimate-order-net-price order))
  (println "Start shipping"))

(defn create-rate-plan []
  {:id (gensym)
   :price 5.0})

(comment
  (create-order)
  (-> (create-order)
      (create-line-item :product-a :rate-plan-a 5 {})
      (create-line-item :product-a :rate-plan-b 5 {}))
  (remove-line-item {:line-items [:a :b :c]} 1)
  (def rate-plan-test (create-rate-plan))
  (-> (create-order)
      (create-line-item :product-a rate-plan-test 5 {})
      (create-line-item :product-b rate-plan-test 10 {})
      (checkout)))
#_(clojure.stacktrace/e)
