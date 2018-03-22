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
