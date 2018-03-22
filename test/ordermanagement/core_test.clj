(ns ordermanagement.core-test
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as s]
            [ordermanagement.core :refer :all]
            [ordermanagement.schema :refer :all]))

(deftest functional-test
  (testing "Create order is conform to initial spec"
    (is (s/valid? :com.hybris.orm.initial/order (create-order))))
  (testing "Create order is not conform to one-item spec"
    (is (not (s/valid? :com.hybris.orm.one-item/order (create-order)))))
  (testing "Create rate plan works"
    (is (s/valid? :ordermanagement.core/rate-plan (create-rate-plan))))
  (testing "Create order and adding an item is conform to initial and one-item spec"
    (is (s/valid? :com.hybris.orm.initial/order (-> (create-order)
                                                    (create-line-item :product-a (create-rate-plan) 5 {}))))
    (is (s/valid? :com.hybris.orm.one-item/order (-> (create-order)
                                                     (create-line-item :product-a (create-rate-plan) 5 {})))))
  (testing "Adding two items of the same product and rate-plan combination will merge them"
    (is (= 2 (count ((let [ rate-plan-a (create-rate-plan) ]
                       (-> (create-order)
                           (create-line-item :product-a rate-plan-a 5 {})
                           (create-line-item :product-a rate-plan-a 2 {})
                           (create-line-item :product-b rate-plan-a 1 {}))) :line-items)))))
  (testing "Removing one item will result in a smaller item list"
    (is (= 1 (count (:line-items (let [ rate-plan-a (create-rate-plan) ]
                       (-> (create-order)
                           (create-line-item :product-a rate-plan-a 5 {})
                           (create-line-item :product-b rate-plan-a 2 {})
                           (remove-line-item 1)))))))))

(deftest scenario-test
  (testing "Create an order, add products and checkout"
    (is (= 4 (count (:line-items (let [rate-plan (create-rate-plan)]
                                    (-> (create-order)
                                        (create-line-item :product-a rate-plan 1 {})
                                        (create-line-item :product-b rate-plan 2 {})
                                        (create-line-item :product-c rate-plan 3 {})
                                        (create-line-item :product-a (create-rate-plan) 1 {})
                                        (create-line-item :product-a rate-plan 1 {})
                                        (checkout)))))))))

(deftest scenario-invoke-test
  (testing "Create an order, add products, remove one and checkout - registry entry default fallback"
    (is (true? (let [order
                     (let [rate-plan (create-rate-plan)]
                       (-> (create-order)
                           (invoke :test :create-line-item :product-a rate-plan 1 {})
                           (invoke :test :create-line-item :product-b rate-plan 2 {})
                           (invoke :test :create-line-item :product-c rate-plan 3 {})
                           (invoke :test :remove-line-item 1)
                           (invoke :test :create-line-item :product-a (create-rate-plan) 1 {})
                           (invoke :test :create-line-item :product-a rate-plan 1 {})
                           (invoke :test :checkout)))]
                 (and (= 3 (count (:line-items order)))
                      (= 30.0 (:net-price order)))))))
  (testing "Create an order, add products, remove one and checkout - registry entry default call"
    (is (true? (let [order
                     (let [rate-plan (create-rate-plan)]
                       (-> (create-order)
                           (invoke :default :create-line-item :product-a rate-plan 1 {})
                           (invoke :default :create-line-item :product-b rate-plan 2 {})
                           (invoke :default :create-line-item :product-c rate-plan 3 {})
                           (invoke :default :remove-line-item 1)
                           (invoke :default :create-line-item :product-a (create-rate-plan) 1 {})
                           (invoke :default :create-line-item :product-a rate-plan 1 {})
                           (invoke :default :checkout)))]
                 (and (= 3 (count (:line-items order)))
                      (= 30.0 (:net-price order))
                      (s/valid? :com.hybris.orm.checkout/order order))))))
  (testing "Create an order, add products, remove one and checkout - registry entry default and fallback"
    (is (true? (let [order
                     (let [rate-plan (create-rate-plan)]
                       (-> (create-order)
                           (invoke :default :create-line-item :product-a rate-plan 1 {})
                           (invoke :default :create-line-item :product-b rate-plan 2 {})
                           (invoke :default :create-line-item :product-c rate-plan 3 {})
                           (invoke :default :remove-line-item 1)
                           (invoke :default :create-line-item :product-a (create-rate-plan) 1 {})
                           (invoke :default :create-line-item :product-a rate-plan 1 {})
                           (invoke :daniel :checkout)))]
                 (and (= 3 (count (:line-items order)))
                      (= 30.0 (:net-price order))
                      (s/valid? :com.hybris.orm.checkout/order order)))))))
