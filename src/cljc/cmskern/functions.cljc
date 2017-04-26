(ns cmskern.functions
  (:require
   [clojure.string :as string]
   #?(:cljs [cljs-time.core :as time]
      :clj [clj-time.core :as time])
   #?(:cljs [cljs-time.coerce :as timeco]
      :clj [clj-time.coerce :as timeco])
   #?(:cljs [cljs-time.format :as timef]
      :clj [clj-time.format :as timef])
   #?(:cljs [cognitect.transit :as t])
   [taoensso.tempura]
   )
  )


(def default-dict {:de
                   {:missing "missing DE"}
                   })
(def opts {:dict default-dict})
(def tr (partial taoensso.tempura/tr opts [:de]))

(defn admin?
  [emp]
  (or (= :admin (:role emp))
      (= "admin" (:role emp))
      (some #{"admin" :admin} (:roles emp))))

(defn vec-remove
  "remove elem in coll"
  [coll pos]
  (vec (concat (subvec coll 0 pos) (subvec coll (inc pos)))))

(defn remove-nil [x]
  (if (map? x)
    (let [kvs (filter (comp not nil? second) x)]
      (if (empty? kvs) nil (into {} kvs)))
    x))

(defn remove-nils
  [m]
  (clojure.walk/postwalk remove-nil m))

;; ---------------------------------------------------------------
;; time functions
;; ---------------------------------------------------------------

(defn time->epoch-int [t]
  (int (timeco/to-epoch t)))


(defn epoch->time [val]
  (when val
    (timeco/from-long val))
  )

(defn timeformat [f t]
  (timef/unparse (timef/formatter f) t)
  )

(defn str->time [tstr f]
  (timef/parse (timef/formatter f) tstr)
  )

(defn epoch->str [e format]
  (timeformat format (timeco/from-long e))
  )

#?(:cljs
   (defn get-value-str
     ""
     [val]
     (or (and (t/tagged-value? val) (= "bson-objectid" (.-tag val)) (.-rep val)) val)))
