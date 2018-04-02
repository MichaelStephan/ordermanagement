(ns ordermanagement.core
  (:require [clojure.spec.alpha :as s]
            [ordermanagement.schema :refer :all]
            [clojure.spec.test.alpha :as stest]
            [datomic.api :as d]))

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
  (let [priced-order (assoc order :net-price (some-> order estimate-order-net-price :net-price))]
    (let [uri "datomic:dev://localhost:4334/ordermanagement"]
      (let [conn (d/connect uri)]
        (let [order-renamed (transform-to-db-order order)]
          (println (d/transact conn [order-renamed])))
        priced-order))))

(defn- pre-validate-merge-line-items [order]
  ; do stuff
)

(defn- post-validate-merge-line-items [order]
  ; do stuff
)

(defn- pre-validate-create-line-item [order]
  (if-not (s/valid? :com.hybris.orm.initial/order order)
    (throw (ex-info (s/explain-str :com.hybris.orm.initial/order order)
                    (s/explain-data :com.hybris.orm.initial/order order)))))

(defn- post-validate-create-line-item [order]
  (if-not (s/valid? :com.hybris.orm.one-item/order order)
    (throw (ex-info (s/explain-str :com.hybris.orm.one-item/order order)
                    (s/explain-data :com.hybris.orm.one-item/order order)))))

(defn- pre-validate-remove-line-item [order]
  ; do stuff
)

(defn- post-validate-remove-line-item [order]
  ; do stuff
)

(defn- pre-validate-estimate-order-net-price [order]
  ; do stuff
)

(defn- post-validate-estimate-order-net-price [order]
  ; do stuff
)

(defn- pre-validate-checkout [order]
  (if-not  (s/valid? :com.hybris.orm.one-item/order order)
    (throw (ex-info (s/explain-str :com.hybris.orm.one-item/order order)
                    (s/explain-data :com.hybris.orm.one-item/order order)))))

(defn- post-validate-checkout [order]
  (if-not  (s/valid? :com.hybris.orm.checkout/order order)
    (throw (ex-info (s/explain-str :com.hybris.orm.checkout/order order)
                    (s/explain-data :com.hybris.orm.checkout/order order)))))

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
               :daniel {:checkout checkout}})

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
    (do
      (if (= :default (keyword customer))
        (fallback func-name))
      (do
        (let [lookup (get registry (keyword customer))]
          (if (contains? lookup (keyword func-name))
            (call lookup func-name)
            (fallback func-name)))
        (fallback func-name)))
    (fallback func-name)))

(defn invoke [order customer func-name & args]
  (let [func (get-func customer func-name)]
    (if-not (fn? func)
      (do
        (throw (Exception. func))
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

(defn transform-to-db-order [order]
  (let [db-order {:order/id (str (get order :id))
                  :order/net-price (or (get order :net-price) 0.0)}]
    (assoc db-order :order/line-item ; assoc not yet working, line-items are not getting attached
           (into []
                 (map (fn [line-item]
                        (let [db-product {:product/id (str (or (get-in line-item [:product-ref :id]) (gensym)))}]
                          (let [db-rate-plan {:rate-plan/id (str (or (get-in line-item [:rate-plan-ref :id])))
                                              :rate-plan/price (get-in line-item [:rate-plan-ref :price])}]
                            (let [db-line-item {:line-item/id (str (or (get line-item :id) (gensym)))
                                                :line-item/config "config of line-item, currently this string"
                                                :line-item/rate-plan db-rate-plan
                                                :line-item/quantity (get line-item :quantity)
                                                :line-item/product db-product}]
                              db-line-item)))) (get order :line-items))))
    db-order))

(comment
  (s/exercise-fn `checkout)
  (stest/summarize-results (stest/check `checkout)))

(comment
  "http://blog.datomic.com/2013/06/component-entities.html - made it work with components, currently products and rate-plans are components as well"
  (let [uri "datomic:dev://localhost:4334/ordermanagement"
        conn (d/connect uri)]
    (d/transact conn [{:order/id "abc"
                       :order/line-item [{:line-item/id "a"
                                          :line-item/config "b"
                                          :line-item/rate-plan {:rate-plan/id "a" :rate-plan/price 10.0}
                                          :line-item/quantity 4
                                          :line-item/product {:product/id "a"}}]}])))

(comment
  (let [uri "datomic:dev://localhost:4334/ordermanagement"]
    (d/create-database uri)
    (let [conn (d/connect uri)]
      (d/transact conn order-schema)
      (d/transact conn rate-plan-schema)
      (d/transact conn product-schema)
      (d/transact conn line-item-schema)))
  (-> (create-order)
      (invoke :default :create-line-item :product-b (create-rate-plan) 2 {})
      (invoke :default :checkout)))

(comment
  (def movie-schema [{:db/ident :movie/title
                      :db/valueType :db.type/string
                      :db/cardinality :db.cardinality/one
                      :db/doc "The title of the movie"}

                     {:db/ident :movie/genre
                      :db/valueType :db.type/string
                      :db/cardinality :db.cardinality/one
                      :db/doc "The genre of the movie"}

                     {:db/ident :movie/cast
                      :db/valueType :db.type/string
                      :db/cardinality :db.cardinality/many
                      :db/doc "Movie cast"}

                     {:db/ident :movie/release-year
                      :db/valueType :db.type/long
                      :db/cardinality :db.cardinality/one
                      :db/doc "The year the movie was released in theaters"}])

  (let [uri "datomic:dev://localhost:4334/ordermanagement"
        conn (d/connect uri)]
    (d/transact conn [{:movie/title "from dusk till dawn"
                       :movie/genre "action"
                       :movie/cast ["quentin" "cloony"]
                       :movie/release-year 2000}])
    #_(d/transact conn movie-schema)
    #_(d/transact conn {:tx-data movie-schema})))
