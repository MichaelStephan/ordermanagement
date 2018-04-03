(ns ordermanagement.schema
  (:require [clojure.spec.alpha :as s]))

(s/def :ordermanagement.core/product-ref keyword?)
(s/def :ordermanagement.core/price (s/and double? #(> % 0)))
(s/def :ordermanagement.core/quantity (s/and int? #(> % 0) #(< % 1e9) #(not (= % nil))))
(s/def :ordermanagement.core/id (s/and any? #(not (= % nil))))
(s/def :ordermanagement.core/config (s/coll-of any?))
(s/def :ordermanagement.core/line-item (s/keys :req-un [:ordermanagement.core/product-ref :ordermanagement.core/rate-plan-ref :ordermanagement.core/quantity]
                           :opt-un [:ordermanagement.core/config]))
(s/def :ordermanagement.core/rate-plan (s/keys :req-un [:ordermanagement.core/id :ordermanagement.core/price]))
(s/def :ordermanagement.core/rate-plan-ref :ordermanagement.core/rate-plan)
(s/def :ordermanagement.core/net-price (s/and double? #(> % 0))) 


(s/def :com.hybris.orm.initial/id :ordermanagement.core/id)
(s/def :com.hybris.orm.initial/line-items (s/* :com.hybris.orm.initial/line-item)) ; 0 or more
(s/def :com.hybris.orm.initial/line-item :ordermanagement.core/line-item)
(s/def :com.hybris.orm.initial/order (s/keys :req-un [:com.hybris.orm.initial/id :com.hybris.orm.initial/line-items]))


(s/def :com.hybris.orm.one-item/id :ordermanagement.core/id)
(s/def :com.hybris.orm.one-item/line-items (s/+ :com.hybris.orm.one-item/line-item)) ; 1 or more
(s/def :com.hybris.orm.one-item/line-item :ordermanagement.core/line-item)
(s/def :com.hybris.orm.one-item/order (s/keys :req-un [:com.hybris.orm.one-item/id :com.hybris.orm.one-item/line-items]))


(s/def :com.hybris.orm.checkout/id :ordermanagement.core/id)
(s/def :com.hybris.orm.checkout/net-price :ordermanagement.core/net-price)
(s/def :com.hybris.orm.checkout/line-items (s/+ :com.hybris.orm.checkout/line-item)) ; 1 or more
(s/def :com.hybris.orm.checkout/line-item :ordermanagement.core/line-item)
(s/def :com.hybris.orm.checkout/order (s/keys :req-un [:com.hybris.orm.checkout/id :com.hybris.orm.checkout/line-items :com.hybris.orm.checkout/net-price]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; function defs


(s/fdef ordermanagement.core/checkout
  :args (s/cat :order :com.hybris.orm.one-item/order)
  :ret :com.hybris.orm.checkout/order)

(s/fdef ordermanagement.core/remove-line-item
        :args (s/cat :order :com.hybris.orm.one-item/order
                     :idx int?)
        :ret :com.hybris.orm.one-item/order
        :fn #(< (count (-> :ret % :order :line-items)) (count (-> % :args :order :line-items))))

(s/fdef ordermanagement.core/create-line-item
        :args (s/and (s/cat :order :com.hybris.orm.initial/order
                            :product-ref :ordermanagement.core/product-ref
                            :rate-plan-ref :ordermanagement.core/rate-plan-ref
                            :quantity int?
                            :config any?))
        :ret :com.hybris.orm.one-item/order
        :fn #(< (+ (count (-> :ret % :order :line-items))) (count (-> % :args :order :line-items)))) ; this should be enhanced, it can only be true if the line items are not merged

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; DB schemas for persistence

(def order-db-keys (array-map :id :order/id
                              :line-items :order/line-item
                              :net-price :order/net-price))

(def line-item-db-keys (array-map :id :line-item/id
                                  :product-ref :line-item/product
                                  :rate-plan-ref :line-item/rate-plan
                                  :quantity :line-item/quantity
                                  :config :line-item/config))

(def product-db-keys (array-map :id :product/id))

(def rate-plan-db-keys (array-map :id :rate-plan/id
                                  :price :rate-plan/price))

(def order-schema [{:db/ident :order/id
                    :db/valueType :db.type/string
                    :db/cardinality :db.cardinality/one
                    :db/doc "The ID of the order"}

                   {:db/ident :order/line-item
                    :db/valueType :db.type/ref
                    :db/cardinality :db.cardinality/many
                    :db/isComponent true
                    :db/doc "The list of line items"}

                   {:db/ident :order/net-price
                    :db/valueType :db.type/double
                    :db/cardinality :db.cardinality/one
                    :db/doc "Net price of the order after being calculated"}])

(def line-item-schema [{:db/ident :line-item/id
                        :db/valueType :db.type/string
                        :db/cardinality :db.cardinality/one
                        :db/doc "The ID of the line item"}

                       {:db/ident :line-item/product
                        :db/valueType :db.type/ref
                        :db/cardinality :db.cardinality/one
                        :db/isComponent true
                        :db/doc "The product contained in this line"}

                       {:db/ident :line-item/rate-plan
                        :db/valueType :db.type/ref
                        :db/cardinality :db.cardinality/one
                        :db/isComponent true
                        :db/doc "The rate-plan this line-item is priced with"}

                       {:db/ident :line-item/quantity
                        :db/valueType :db.type/long
                        :db/cardinality :db.cardinality/one
                        :db/doc "The amount of products referenced to the rate-plan for price calculation"}

                       {:db/ident :line-item/config
                        :db/valueType :db.type/string
                        :db/cardinality :db.cardinality/many
                        :db/doc "line item configuration, currently not specified in more detail"}])

(def product-schema [{:db/ident :product/id
                      :db/valueType :db.type/string
                      :db/cardinality :db.cardinality/one
                      :db/doc "The ID of the product"}])

(def rate-plan-schema [{:db/ident :rate-plan/id
                        :db/valueType :db.type/string
                        :db/cardinality :db.cardinality/one
                        :db/doc "The ID of the rate-plan"}

                       {:db/ident :rate-plan/price
                        :db/valueType :db.type/double
                        :db/cardinality :db.cardinality/one
                        :db/doc "The price of an item in a rate-plan"}])
