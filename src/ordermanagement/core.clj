(ns ordermanagement.core)

(defn create-order []
  {:id (gensym)
   :line-items []})

(def safe+ (fnil + 0 0))
(def safe* (fnil * 0 0))

(defn merge-line-items [{:keys [line-items] :as order}]
  (assoc order :line-items
         (into []
               (for [[k v] (group-by (fn [{:keys [product-ref rate-plan-ref]}] (str product-ref rate-plan-ref)) line-items)]
                 (reduce (fn [ret line-item]
                           (merge ret line-item {:quantity (safe+ (:quantity ret) (:quantity line-item))})) {}  v)))))

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

(defn- estimate-line-item-net-price [line-item]
  (safe* (-> line-item :rate-plan-ref :price) (line-item :quantity)))

(defn estimate-order-net-price [order]
  (assoc order :net-price (reduce + (map estimate-line-item-net-price (order :line-items)))))

(defn checkout [order]
  (println "Make user pay $" (some-> order estimate-order-net-price :net-price))
  (println "Items in cart:" (count (:line-items order)))
  (println "Start shipping"))


(defn registry {:default {:merge-line-items merge-line-items
                          :create-line-item create-line-item
                          :remove-line-item remove-line-item
                          :estimate-order-net-price estimate-order-net-price
                          :checkout checkout} 
                :customer_a {:checkout customer-a-checkout} 
                :customer_b {:create-line-item customer-b-create-line-item}})

(defn invoke [customer func-name & args]
  resolve function name to function handle
  validate spec pre 
  apply function handle 
  validate spec post
  return)

(defn create-rate-plan []
  {:id (gensym)
   :price 5.0})

(comment
  (create-order)
  (-> (create-order)
      (create-line-item :product-a :rate-plan-a 5 {})
      (create-line-item :product-a :rate-plan-b 5 {}))
  (remove-line-item {:line-items [:a :b :c]} 1)
  (let [rate-plan-test (create-rate-plan)]
    (-> (create-order)                                        ; create an order
        (create-line-item :product-a rate-plan-test 5 {})     ; add item to order
        (create-line-item :product-b rate-plan-test 10 {})    ; add another item to order
        (remove-line-item 1)                                  ; remove line item with index 1
        (create-line-item :product-c rate-plan-test 3 {})     ; add another item to order
        (create-line-item :product-c rate-plan-test 2 {})     ; add line item of same product type
        (checkout))))
#_(clojure.stacktrace/e)
