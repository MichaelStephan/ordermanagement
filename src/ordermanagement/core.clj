(ns ordermanagement.core
  (:require [clojure.spec.alpha :as s]))

(s/def ::product-ref keyword?)
(s/def ::price (s/and double? #(> % 0)))
(s/def ::quantity (s/and int? #(> % 0)))
(s/def ::id any?)
(s/def ::config (s/coll-of any?))
(s/def ::line-item (s/keys :req-un [::product-ref ::rate-plan-ref ::quantity]
                           :opt-un [::config]))
(s/def ::rate-plan (s/keys :req-un [::id ::price]))
(s/def ::rate-plan-ref ::rate-plan)


(s/def :com.hybris.orm.initial/id ::id)
(s/def :com.hybris.orm.initial/line-items (s/* :com.hybris.orm.initial/line-item)) ; 0 or more
(s/def :com.hybris.orm.initial/line-item ::line-item)
(s/def :com.hybris.orm.initial/order (s/keys :req-un [::id :com.hybris.orm.initial/line-items]))


(s/def :com.hybris.orm.one-item/id ::id)
(s/def :com.hybris.orm.one-item/line-items (s/+ :com.hybris.orm.one-item/line-item)) ; 1 or more
(s/def :com.hybris.orm.one-item/line-item ::line-item)
(s/def :com.hybris.orm.one-item/order (s/keys :req-un [::id :com.hybris.orm.one-item/line-items]))


(s/def :com.hybris.orm.checkout/id ::id)
(s/def :com.hybris.orm.checkout/line-items (s/+ :com.hybris.orm.checkout/line-item)) ; 1 or more
(s/def :com.hybris.orm.checkout/line-item ::line-item)
(s/def :com.hybris.orm.checkout/order (s/keys :req-un [::id :com.hybris.orm.checkout/line-items]))

(println (s/exercise :com.hybris.orm.one-item/order))

(s/fdef checkout
  :args :com.hybris.orm.one-item/order
  :ret :com.hybris.orm.checkout/order)

(s/exercise-fn `checkout)

(s/fdef remove-line-item
        :args :com.hybris.orm.one-item/order
        :ret :com.hybris.orm.one-item/order
        :fn #(< (count (-> :ret % :line-items)) (count (-> % :args :line-items))))

;(s/exercise-fn `remove-line-item)

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
  (println "Start shipping")
  order)

(defn- pre-validate-merge-line-items [order]
  (println "pre-validate-merge-line-items"))

(defn- post-validate-merge-line-items [order]
  (println "post-validate-merge-line-items"))

(defn- pre-validate-create-line-item [order]
  (println "pre-validate-create-line-item")
  (if-not (s/valid? :com.hybris.orm.initial/order order)
    (throw (ex-info (s/explain-str :com.hybris.orm.initial/order order)
                    (s/explain-data :com.hybris.orm.initial/order order)))))

(defn- post-validate-create-line-item [order]
  (println "post-validate-create-line-item")
  (if-not (s/valid? :com.hybris.orm.one-item/order order)
    (throw (ex-info (s/explain-str :com.hybris.orm.one-item/order order)
                    (s/explain-data :com.hybris.orm.one-item/order order)))))

(defn- pre-validate-remove-line-item [order]
  (println "pre-validate-remove-line-item"))

(defn- post-validate-remove-line-item [order]
  (println "post-validate-remove-line-item"))

(defn- pre-validate-estimate-order-net-price [order]
  (println "pre-validate-estimate-order-net-price"))

(defn- post-validate-estimate-order-net-price [order]
  (println "post-validate-estimate-order-net-price"))

(defn- pre-validate-checkout [order]
  (println "pre-validate-checkout")
  (if-not  (s/valid? :com.hybris.orm.one-item/order order)
    (throw (ex-info (s/explain-str :com.hybris.orm.one-item/order order)
                    (s/explain-data :com.hybris.orm.one-item/order order)))))

(defn- post-validate-checkout [order]
  (println "post-validate-checkout")
  (if-not  (s/valid? :com.hybris.orm.one-item/order order)
    (throw (ex-info (s/explain-str :com.hybris.orm.one-item/order order)
                    (s/explain-data :com.hybris.orm.one-item/order order)))))

(def registry {:default {:merge-line-items {:pre-validate pre-validate-merge-line-items
                                            :func merge-line-items
                                            :post-validate post-validate-merge-line-items}
                          :create-line-item {:pre-validate pre-validate-create-line-item
                                             :func create-line-item
                                             :post-validate post-validate-create-line-item}
                          :remove-line-item {:pre-validate pre-validate-remove-line-item
                                             :func remove-line-item
                                             :post-validate post-validate-remove-line-item}
                          :estimate-order-net-price {:pre-validate pre-validate-estimate-order-net-price
                                                     :func estimate-order-net-price
                                                     :post-validate post-validate-estimate-order-net-price}
                          :checkout {:pre-validate pre-validate-checkout
                                     :func checkout
                                     :post-validate post-validate-checkout}}
               :daniel { :checkout checkout}})
                ;:customer_a {:checkout customer-a-checkout} 
                ;:customer_b {:create-line-item customer-b-create-line-item}})

(defn- call [reg fname]
  (let [func ((keyword fname) reg)]
    func))

(defn- fallback [func-name]
  (let [default (get registry :default)]
    (if (contains? default (keyword func-name))
      (call (get-in default [(keyword func-name)]) :func)
      (str "function doesn't exist in scope"))))

(defn- get-func [customer func-name]
  (if (contains? registry (keyword customer))
    (let [lookup (get registry (keyword customer))]
      (if (contains? lookup (keyword func-name))
        (call lookup func-name)
        (fallback func-name)))
    (fallback func-name)))

(defn invoke [order customer func-name & args]
  (let [func (get-func customer func-name)]
        (if-not (fn? func)
          (do
            (throw (Exception. func))
            (println func)
            order)
          (do
            (-> (call (get-in registry [:default (keyword func-name)]) :pre-validate)
                (apply [order]))                                                            ; call pre validate
            (let [ord (apply func order args)]                                              ; call actual function
              (-> (call (get-in registry [:default (keyword func-name)]) :post-validate)
                (apply [ord]))                                                              ; call post validate
              ord)))))

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
        (invoke :daniel :test)))                                               ; test function that doesn't exist throw exception
  (let [rate-plan-test (create-rate-plan)]
    (-> (create-order)
        (invoke :daniel :checkout))))
#_(clojure.stacktrace/e)
