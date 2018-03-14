(ns ordermanagement.core
  (:require [clojure.spec.alpha :as s]))

(s/def ::order (s/keys :req-un [::id ::line-items]     ; req-un makes it so, that the namespace requirement is no longer necessary
                       :opt-un []))

(s/def ::line-items (s/* ::line-item))                                                                                                                                                                                     

(s/def ::line-item (s/cat :product-ref keyword?                                                                                                                                                                            
                   :rate-plan-ref keyword?                                                                                                                                                                          
                   :quantity number?                                                                                                                                                                                
                   :config keyword?))  

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




(def registry {:default {:merge-line-items merge-line-items
                          :create-line-item create-line-item
                          :remove-line-item remove-line-item
                          :estimate-order-net-price estimate-order-net-price
                          :checkout checkout}
               :daniel { :checkout checkout}})
                ;:customer_a {:checkout customer-a-checkout} 
                ;:customer_b {:create-line-item customer-b-create-line-item}})

(defn- call [reg fname]
  (let [func ((keyword fname) reg)]
    func))

(defn- fallback [func-name]
  (let [default (get registry :default)]
    (if (contains? default (keyword func-name))
      (call default func-name)
      (str "function doesn't exist in scope"))))

(defn- get-func [customer func-name]
  (if (contains? registry (keyword customer))
    (let [lookup (get registry (keyword customer))]
      (if (contains? lookup (keyword func-name))
        (call lookup func-name)
        (fallback func-name)))
    (fallback func-name)))

(defn pre-validate [order]
  ,,,)

(defn post-validate [order]
  ,,,)

(defn invoke [order customer func-name & args]
  (let [func (get-func customer func-name)]
        (if-not (fn? func)
          (do
            (throw (Exception. func))
            (println func)
            args)
          (do
            (pre-validate args)
            (apply func order args)
            (post-validate args))))
  ;(if-not (s/valid? ::order args)
  ;  (throw (ex-info (s/explain-str ::order args)
  ;                  (s/explain-data ::order args))))
  ;resolve function name to function handle
  ;validate spec pre 
  ;apply function handle 
  ;validate spec post
  ;return
  ;
  ; call function by string name: (apply (resolve (symbol func-name)) args)
  ,,,)

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
        (checkout)))
  (let [rate-plan-test (create-rate-plan)]
    (-> (create-order)                                                         ; create an order
        (invoke :daniel :create-line-item :product-a rate-plan-test 5 {})      ; add item to order
        (invoke :daniel :create-line-item :product-b rate-plan-test 10 {})     ; add another item to order
        (invoke :daniel :remove-line-item 1)                                   ; remove line item with index 1
        (invoke :daniel :create-line-item :product-c rate-plan-test 3 {})      ; add another item to order
        (invoke :daniel :create-line-item :product-c rate-plan-test 2 {})      ; add line item of same product type
        (invoke :daniel-nonexistent :checkout)                                 ; checkout with override function
        (invoke :daniel :checkout)                                             ; checkout with default function
        (invoke :daniel :test))))                                              ; test function that doesn't exist throw exception
#_(clojure.stacktrace/e)
