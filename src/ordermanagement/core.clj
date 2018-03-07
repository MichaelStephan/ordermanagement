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

(defn estimate-line-item-net-price [line-item])

(defn estimate-order-net-price [order])

(defn checkout [order])


(comment
  (create-order)
  (-> (create-order)
      (create-line-item :product-a :rate-plan-a 5 {})
      (create-line-item :product-a :rate-plan-b 5 {}))
  (remove-line-item {:line-items [:a :b :c]} 1))

#_(clojure.stacktrace/e)
