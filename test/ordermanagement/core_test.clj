(ns ordermanagement.core-test
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as s]
            [ordermanagement.core :refer :all]))

(deftest functional-test
  (testing "Create order is conform to initial spec"
    (is (s/valid? :com.hybris.orm.initial/order (create-order)))))

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

