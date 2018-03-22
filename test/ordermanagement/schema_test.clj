(ns ordermanagement.schema-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :as pprint]
            [ordermanagement.core :refer :all]
            [ordermanagement.schema :refer :all]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]))

; Instrument will tell us if we violate the spec when calling a function
(stest/instrument `checkout)
(stest/instrument `create-line-item)
(stest/instrument `remove-line-item)

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
  (testing "Checkout order shall have net price"
    (is (s/valid? :com.hybris.orm.checkout/order (checkout (create-line-item (create-order) :product-a (create-rate-plan) 5 {}))))))

(deftest schema-function-test
  (testing "Generated data should not break create-line-item"
    (is (not (nil? (s/exercise-fn `create-line-item))))
    (is (not (nil? (s/exercise-fn `remove-line-item))))
    (is (not (nil? (s/exercise-fn `checkout))))))

(comment
  "Only manually execute these functions"
  (-> `checkout stest/check stest/summarize-results) ; careful executing this line, takes about 5 minutes until it runs in an OutOfMemoryException
  (-> `create-line-item stest/check stest/summarize-results)
  (-> `remove-line-item stest/check stest/summarize-results))
