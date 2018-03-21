(ns ordermanagement.schema-test
  (:require [clojure.test :refer :all]
            [ordermanagement.schema]
            [ordermanagement.core :refer :all]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]))

(deftest schema-def-test
  (testing "ID spec"
    (is (false? (s/valid? :ordermanagement.core/id nil))))
  (testing "Quantity spec"
    (is (false? (s/valid? :ordermanagement.core/quantity nil)))
    (is (false? (s/valid? :ordermanagement.core/quantity "test")))
    (is (false? (s/valid? :ordermanagement.core/quantity {})))
    (is (false? (s/valid? :ordermanagement.core/quantity 0)))
    (is (false? (s/valid? :ordermanagement.core/quantity 1e9)))
    (is (true? (s/valid? :ordermanagement.core/quantity 5))))
  (testing "Product-ref spec"
    (is (false? (s/valid? :ordermanagement.core/product-ref nil)))
    (is (false? (s/valid? :ordermanagement.core/product-ref "test")))
    (is (false? (s/valid? :ordermanagement.core/product-ref {})))
    (is (false? (s/valid? :ordermanagement.core/product-ref 0)))
    (is (true? (s/valid? :ordermanagement.core/product-ref :keyword))))
  (testing "Price spec")
  (testing "Rate-plan spec")
  (testing "Config spec")
  (testing "Line-item spec")
  (testing "Create order is conform to initial spec"
    (is (s/valid? :com.hybris.orm.initial/order (create-order))))
  (testing "Create line item will create an order with at least one item"
    (is (s/valid? :com.hybris.orm.one-item/order (create-line-item (create-order) :product-a (create-rate-plan) 5 {}))))
  (testing "Remove line item will work for an order with one item"
    (is (s/valid? :com.hybris.orm.initial/order (remove-line-item (create-line-item (create-order) :product-a (create-rate-plan) 5 {}) 1))))
  (testing "Remove line item will work for an order with two items and still have one item"
    (is (s/valid? :com.hybris.orm.one-item/order (remove-line-item (create-line-item (create-line-item (create-order) :product-a (create-rate-plan) 5 {})  :product-b (create-rate-plan) 5 {}) 1))))
  (testing "Remove line item will not work for an order with no item"
    (is (s/valid? :com.hybris.orm.initial/order (remove-line-item (create-order) 1)))))

(deftest schema-function-test
  (testing "Generated data should not break create-line-item"
    (is (not (nil? (s/exercise-fn `create-line-item))))))


(comment
  (stest/check `checkout) ; careful executing this line, takes about 5 minutes until it runs in an OutOfMemoryException
  (s/exercise-fn `create-line-item)
  (s/exercise-fn `remove-line-item)
  (s/exercise-fn `checkout))
