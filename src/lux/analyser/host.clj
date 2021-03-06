;;   Copyright (c) Eduardo Julian. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns lux.analyser.host
  (:require (clojure [template :refer [do-template]])
            [clojure.core.match :as M :refer [match matchv]]
            clojure.core.match.array
            (lux [base :as & :refer [|let |do return fail]]
                 [parser :as &parser]
                 [type :as &type]
                 [host :as &host])
            (lux.analyser [base :as &&]
                          [env :as &&env])))

;; [Utils]
(defn ^:private extract-text [text]
  (matchv ::M/objects [text]
    [["lux;Meta" [_ ["lux;TextS" ?text]]]]
    (return ?text)

    [_]
    (fail "[Analyser Error] Can't extract Text.")))

(defn ^:private analyse-1+ [analyse ?token]
  (&type/with-var
    (fn [$var]
      (|do [=expr (&&/analyse-1 analyse $var ?token)]
        (matchv ::M/objects [=expr]
          [[?item ?type]]
          (|do [=type (&type/clean $var ?type)]
            (return (&/T ?item =type)))
          )))))

(defn ^:private ensure-object [token]
  "(-> Analysis (Lux (,)))"
  (matchv ::M/objects [token]
    [[_ ["lux;DataT" _]]]
    (return nil)

    [_]
    (fail "[Analyser Error] Expecting object")))

(defn ^:private as-object [type]
  "(-> Type Type)"
  (matchv ::M/objects [type]
    [["lux;DataT" class]]
    (&/V "lux;DataT" (&type/as-obj class))

    [_]
    type))

;; [Resources]
(do-template [<name> <output-tag> <input-class> <output-class>]
  (let [input-type (&/V "lux;DataT" <input-class>)
        output-type (&/V "lux;DataT" <output-class>)]
    (defn <name> [analyse exo-type ?x ?y]
      (|do [=x (&&/analyse-1 analyse input-type ?x)
            =y (&&/analyse-1 analyse input-type ?y)
            _ (&type/check exo-type output-type)]
        (return (&/|list (&/T (&/V <output-tag> (&/T =x =y)) output-type))))))

  analyse-jvm-iadd "jvm-iadd" "java.lang.Integer" "java.lang.Integer"
  analyse-jvm-isub "jvm-isub" "java.lang.Integer" "java.lang.Integer"
  analyse-jvm-imul "jvm-imul" "java.lang.Integer" "java.lang.Integer"
  analyse-jvm-idiv "jvm-idiv" "java.lang.Integer" "java.lang.Integer"
  analyse-jvm-irem "jvm-irem" "java.lang.Integer" "java.lang.Integer"
  analyse-jvm-ieq  "jvm-ieq"  "java.lang.Integer" "java.lang.Boolean"
  analyse-jvm-ilt  "jvm-ilt"  "java.lang.Integer" "java.lang.Boolean"
  analyse-jvm-igt  "jvm-igt"  "java.lang.Integer" "java.lang.Boolean"

  analyse-jvm-ceq  "jvm-ceq"  "java.lang.Character" "java.lang.Boolean"
  analyse-jvm-clt  "jvm-clt"  "java.lang.Character" "java.lang.Boolean"
  analyse-jvm-cgt  "jvm-cgt"  "java.lang.Character" "java.lang.Boolean"

  analyse-jvm-ladd "jvm-ladd" "java.lang.Long"    "java.lang.Long"
  analyse-jvm-lsub "jvm-lsub" "java.lang.Long"    "java.lang.Long"
  analyse-jvm-lmul "jvm-lmul" "java.lang.Long"    "java.lang.Long"
  analyse-jvm-ldiv "jvm-ldiv" "java.lang.Long"    "java.lang.Long"
  analyse-jvm-lrem "jvm-lrem" "java.lang.Long"    "java.lang.Long"
  analyse-jvm-leq  "jvm-leq"  "java.lang.Long"    "java.lang.Boolean"
  analyse-jvm-llt  "jvm-llt"  "java.lang.Long"    "java.lang.Boolean"
  analyse-jvm-lgt  "jvm-lgt"  "java.lang.Long"    "java.lang.Boolean"

  analyse-jvm-fadd "jvm-fadd" "java.lang.Float"   "java.lang.Float"
  analyse-jvm-fsub "jvm-fsub" "java.lang.Float"   "java.lang.Float"
  analyse-jvm-fmul "jvm-fmul" "java.lang.Float"   "java.lang.Float"
  analyse-jvm-fdiv "jvm-fdiv" "java.lang.Float"   "java.lang.Float"
  analyse-jvm-frem "jvm-frem" "java.lang.Float"   "java.lang.Float"
  analyse-jvm-feq  "jvm-feq"  "java.lang.Float"   "java.lang.Boolean"
  analyse-jvm-flt  "jvm-flt"  "java.lang.Float"   "java.lang.Boolean"
  analyse-jvm-fgt  "jvm-fgt"  "java.lang.Float"   "java.lang.Boolean"

  analyse-jvm-dadd "jvm-dadd" "java.lang.Double"  "java.lang.Double"
  analyse-jvm-dsub "jvm-dsub" "java.lang.Double"  "java.lang.Double"
  analyse-jvm-dmul "jvm-dmul" "java.lang.Double"  "java.lang.Double"
  analyse-jvm-ddiv "jvm-ddiv" "java.lang.Double"  "java.lang.Double"
  analyse-jvm-drem "jvm-drem" "java.lang.Double"  "java.lang.Double"
  analyse-jvm-deq  "jvm-deq"  "java.lang.Double"  "java.lang.Boolean"
  analyse-jvm-dlt  "jvm-dlt"  "java.lang.Double"  "java.lang.Boolean"
  analyse-jvm-dgt  "jvm-dgt"  "java.lang.Double"  "java.lang.Boolean"
  )

(defn analyse-jvm-getstatic [analyse exo-type ?class ?field]
  (|do [class-loader &/loader
        =type (&host/lookup-static-field class-loader ?class ?field)
        :let [output-type =type]
        _ (&type/check exo-type output-type)]
    (return (&/|list (&/T (&/V "jvm-getstatic" (&/T ?class ?field)) output-type)))))

(defn analyse-jvm-getfield [analyse exo-type ?class ?field ?object]
  (|do [class-loader &/loader
        =type (&host/lookup-static-field class-loader ?class ?field)
        =object (&&/analyse-1 analyse ?object)
        :let [output-type =type]
        _ (&type/check exo-type output-type)]
    (return (&/|list (&/T (&/V "jvm-getfield" (&/T ?class ?field =object)) output-type)))))

(defn analyse-jvm-putstatic [analyse exo-type ?class ?field ?value]
  (|do [class-loader &/loader
        =type (&host/lookup-static-field class-loader ?class ?field)
        =value (&&/analyse-1 analyse =type ?value)
        :let [output-type &type/Unit]
        _ (&type/check exo-type output-type)]
    (return (&/|list (&/T (&/V "jvm-putstatic" (&/T ?class ?field =value)) output-type)))))

(defn analyse-jvm-putfield [analyse exo-type ?class ?field ?object ?value]
  (|do [class-loader &/loader
        =type (&host/lookup-static-field class-loader ?class ?field)
        =object (&&/analyse-1 analyse ?object)
        =value (&&/analyse-1 analyse =type ?value)
        :let [output-type &type/Unit]
        _ (&type/check exo-type output-type)]
    (return (&/|list (&/T (&/V "jvm-putfield" (&/T ?class ?field =object =value)) output-type)))))

(defn analyse-jvm-invokestatic [analyse exo-type ?class ?method ?classes ?args]
  (|do [class-loader &/loader
        =classes (&/map% extract-text ?classes)
        =return (&host/lookup-static-method class-loader ?class ?method =classes)
        ;; :let [_ (matchv ::M/objects [=return]
        ;;           [["lux;DataT" _return-class]]
        ;;           (prn 'analyse-jvm-invokestatic ?class ?method _return-class))]
        =args (&/map2% (fn [_class _arg]
                         (&&/analyse-1 analyse (&/V "lux;DataT" _class) _arg))
                       =classes
                       ?args)
        :let [output-type =return]
        _ (&type/check exo-type output-type)]
    (return (&/|list (&/T (&/V "jvm-invokestatic" (&/T ?class ?method =classes =args)) output-type)))))

(defn analyse-jvm-instanceof [analyse exo-type ?class ?object]
  (|do [=object (analyse-1+ analyse ?object)
        _ (ensure-object =object)
        :let [output-type &type/Bool]
        _ (&type/check exo-type output-type)]
    (return (&/|list (&/T (&/V "jvm-instanceof" (&/T ?class =object)) output-type)))))

(do-template [<name> <tag>]
  (defn <name> [analyse exo-type ?class ?method ?classes ?object ?args]
    (|do [class-loader &/loader
          =classes (&/map% extract-text ?classes)
          =return (&host/lookup-virtual-method class-loader ?class ?method =classes)
          =object (&&/analyse-1 analyse (&/V "lux;DataT" ?class) ?object)
          =args (&/map2% (fn [?c ?o] (&&/analyse-1 analyse (&/V "lux;DataT" ?c) ?o))
                         =classes ?args)
          :let [output-type =return]
          _ (&type/check exo-type output-type)]
      (return (&/|list (&/T (&/V <tag> (&/T ?class ?method =classes =object =args)) output-type)))))

  analyse-jvm-invokevirtual   "jvm-invokevirtual"
  analyse-jvm-invokeinterface "jvm-invokeinterface"
  )

(defn analyse-jvm-invokespecial [analyse exo-type ?class ?method ?classes ?object ?args]
  (|do [class-loader &/loader
        =classes (&/map% extract-text ?classes)
        =return (if (= "<init>" ?method)
                  (return &type/Unit)
                  (&host/lookup-virtual-method class-loader ?class ?method =classes))
        =object (&&/analyse-1 analyse (&/V "lux;DataT" ?class) ?object)
        =args (&/map2% (fn [?c ?o]
                         (&&/analyse-1 analyse (&/V "lux;DataT" ?c) ?o))
                       =classes ?args)
        :let [output-type =return]
        _ (&type/check exo-type output-type)]
    (return (&/|list (&/T (&/V "jvm-invokespecial" (&/T ?class ?method =classes =object =args)) output-type)))))

(defn analyse-jvm-null? [analyse exo-type ?object]
  (|do [=object (analyse-1+ analyse ?object)
        _ (ensure-object =object)
        :let [output-type &type/Bool]
        _ (&type/check exo-type output-type)]
    (return (&/|list (&/T (&/V "jvm-null?" =object) output-type)))))

(defn analyse-jvm-null [analyse exo-type]
  (|do [:let [output-type (&/V "lux;DataT" "null")]
        _ (&type/check exo-type output-type)]
    (return (&/|list (&/T (&/V "jvm-null" nil) output-type)))))

(defn analyse-jvm-new [analyse exo-type ?class ?classes ?args]
  (|do [=classes (&/map% extract-text ?classes)
        =args (&/map% (partial analyse-1+ analyse) ?args)
        :let [output-type (&/V "lux;DataT" ?class)]
        _ (&type/check exo-type output-type)]
    (return (&/|list (&/T (&/V "jvm-new" (&/T ?class =classes =args)) output-type)))))

(defn analyse-jvm-new-array [analyse ?class ?length]
  (return (&/|list (&/T (&/V "jvm-new-array" (&/T ?class ?length)) (&/V "array" (&/T (&/V "lux;DataT" ?class)
                                                                                     (&/V "lux;Nil" nil)))))))

(defn analyse-jvm-aastore [analyse ?array ?idx ?elem]
  (|do [=array (analyse-1+ analyse ?array)
        =elem (analyse-1+ analyse ?elem)
        =array-type (&&/expr-type =array)]
    (return (&/|list (&/T (&/V "jvm-aastore" (&/T =array ?idx =elem)) =array-type)))))

(defn analyse-jvm-aaload [analyse ?array ?idx]
  (|do [=array (analyse-1+ analyse ?array)
        =array-type (&&/expr-type =array)]
    (return (&/|list (&/T (&/V "jvm-aaload" (&/T =array ?idx)) =array-type)))))

(defn ^:private analyse-modifiers [modifiers]
  (&/fold% (fn [so-far modif]
             (matchv ::M/objects [modif]
               [["lux;Meta" [_ ["lux;TextS" "public"]]]]
               (return (assoc so-far :visibility "public"))

               [["lux;Meta" [_ ["lux;TextS" "private"]]]]
               (return (assoc so-far :visibility "private"))

               [["lux;Meta" [_ ["lux;TextS" "protected"]]]]
               (return (assoc so-far :visibility "protected"))

               [["lux;Meta" [_ ["lux;TextS" "static"]]]]
               (return (assoc so-far :static? true))

               [["lux;Meta" [_ ["lux;TextS" "final"]]]]
               (return (assoc so-far :final? true))

               [["lux;Meta" [_ ["lux;TextS" "abstract"]]]]
               (return (assoc so-far :abstract? true))

               [["lux;Meta" [_ ["lux;TextS" "synchronized"]]]]
               (return (assoc so-far :concurrency "synchronized"))

               [["lux;Meta" [_ ["lux;TextS" "volatile"]]]]
               (return (assoc so-far :concurrency "volatile"))

               [_]
               (fail (str "[Analyser Error] Unknown modifier: " (&/show-ast modif)))))
           {:visibility "default"
            :static? false
            :final? false
            :abstract? false
            :concurrency nil}
           modifiers))

(defn ^:private as-otype [tname]
  (case tname
    "boolean" "java.lang.Boolean"
    "byte"    "java.lang.Byte"
    "short"   "java.lang.Short"
    "int"     "java.lang.Integer"
    "long"    "java.lang.Long"
    "float"   "java.lang.Float"
    "double"  "java.lang.Double"
    "char"    "java.lang.Character"
    ;; else
    tname
    ))

(defn analyse-jvm-class [analyse ?name ?super-class ?interfaces ?fields ?methods]
  (|do [=interfaces (&/map% extract-text ?interfaces)
        =fields (&/map% (fn [?field]
                          (matchv ::M/objects [?field]
                            [["lux;Meta" [_ ["lux;FormS" ["lux;Cons" [["lux;Meta" [_ ["lux;TextS" ?field-name]]]
                                                                      ["lux;Cons" [["lux;Meta" [_ ["lux;TextS" ?field-type]]]
                                                                                   ["lux;Cons" [["lux;Meta" [_ ["lux;TupleS" ?field-modifiers]]]
                                                                                                ["lux;Nil" _]]]]]]]]]]]
                            (|do [=field-modifiers (analyse-modifiers ?field-modifiers)]
                              (return {:name ?field-name
                                       :modifiers =field-modifiers
                                       :type ?field-type}))
                            
                            [_]
                            (fail "[Analyser Error] Wrong syntax for field.")))
                        ?fields)
        =methods (&/map% (fn [?method]
                           (matchv ::M/objects [?method]
                             [[?idx ["lux;Meta" [_ ["lux;FormS" ["lux;Cons" [["lux;Meta" [_ ["lux;TextS" ?method-name]]]
                                                                             ["lux;Cons" [["lux;Meta" [_ ["lux;TupleS" ?method-inputs]]]
                                                                                          ["lux;Cons" [["lux;Meta" [_ ["lux;TextS" ?method-output]]]
                                                                                                       ["lux;Cons" [["lux;Meta" [_ ["lux;TupleS" ?method-modifiers]]]
                                                                                                                    ["lux;Cons" [?method-body
                                                                                                                                 ["lux;Nil" _]]]]]]]]]]]]]]]]
                             (|do [=method-inputs (&/map% (fn [minput]
                                                            (matchv ::M/objects [minput]
                                                              [["lux;Meta" [_ ["lux;FormS" ["lux;Cons" [["lux;Meta" [_ ["lux;SymbolS" ?input-name]]]
                                                                                                        ["lux;Cons" [["lux;Meta" [_ ["lux;TextS" ?input-type]]]
                                                                                                                     ["lux;Nil" _]]]]]]]]]
                                                              (return (&/T (&/ident->text ?input-name) ?input-type))

                                                              [_]
                                                              (fail "[Analyser Error] Wrong syntax for method input.")))
                                                          ?method-inputs)
                                   =method-modifiers (analyse-modifiers ?method-modifiers)
                                   =method-body (&/with-scope (str ?name "_" ?idx)
                                                  (&/fold (fn [body* input*]
                                                            (|let [[iname itype] input*]
                                                              (&&env/with-local iname (&/V "lux;DataT" (as-otype itype))
                                                                body*)))
                                                          (if (= "void" ?method-output)
                                                            (analyse-1+ analyse ?method-body)
                                                            (&&/analyse-1 analyse (&/V "lux;DataT" (as-otype ?method-output)) ?method-body))
                                                          (&/|reverse (if (:static? =method-modifiers)
                                                                        =method-inputs
                                                                        (&/|cons (&/T ";this" ?super-class)
                                                                                 =method-inputs)))))]
                               (return {:name ?method-name
                                        :modifiers =method-modifiers
                                        :inputs (&/|map &/|second =method-inputs)
                                        :output ?method-output
                                        :body =method-body}))
                             
                             [_]
                             (fail "[Analyser Error] Wrong syntax for method.")))
                         (&/enumerate ?methods))]
    (return (&/|list (&/V "jvm-class" (&/T ?name ?super-class =interfaces =fields =methods))))))

(defn analyse-jvm-interface [analyse ?name ?supers ?methods]
  (|do [=supers (&/map% extract-text ?supers)
        =methods (&/map% (fn [method]
                           (matchv ::M/objects [method]
                             [["lux;Meta" [_ ["lux;FormS" ["lux;Cons" [["lux;Meta" [_ ["lux;TextS" ?method-name]]]
                                                                       ["lux;Cons" [["lux;Meta" [_ ["lux;TupleS" ?inputs]]]
                                                                                    ["lux;Cons" [["lux;Meta" [_ ["lux;TextS" ?output]]]
                                                                                                 ["lux;Cons" [["lux;Meta" [_ ["lux;TupleS" ?modifiers]]]
                                                                                                              ["lux;Nil" _]]]]]]]]]]]]]
                             (|do [=inputs (&/map% extract-text ?inputs)
                                   =modifiers (analyse-modifiers ?modifiers)]
                               (return {:name ?method-name
                                        :modifiers =modifiers
                                        :inputs =inputs
                                        :output ?output}))
                             
                             [_]
                             (fail (str "[Analyser Error] Invalid method signature: " (&/show-ast method)))))
                         ?methods)]
    (return (&/|list (&/V "jvm-interface" (&/T ?name =supers =methods))))))

(defn analyse-jvm-try [analyse exo-type ?body ?catches+?finally]
  (|do [:let [[?catches ?finally] ?catches+?finally]
        =body (&&/analyse-1 analyse exo-type ?body)
        =catches (&/map% (fn [[?ex-class ?ex-arg ?catch-body]]
                           (|do [=catch-body (&&env/with-local (str ";" ?ex-arg) (&/V "lux;DataT" ?ex-class)
                                               (&&/analyse-1 analyse exo-type ?catch-body))
                                 idx &&env/next-local-idx]
                             (return (&/T ?ex-class idx =catch-body))))
                         ?catches)
        =finally (matchv ::M/objects [?finally]
                   [["lux;None" _]] (return (&/V "lux;None" nil))
                   [["lux;Some" ?finally*]] (|do [=finally (analyse-1+ analyse ?finally*)]
                                              (return (&/V "lux;Some" =finally))))]
    (return (&/|list (&/T (&/V "jvm-try" (&/T =body =catches =finally)) exo-type)))))

(defn analyse-jvm-throw [analyse exo-type ?ex]
  (|do [=ex (analyse-1+ analyse ?ex)
        :let [[_obj _type] =ex]
        _ (&type/check (&/V "lux;DataT" "java.lang.Throwable") _type)]
    (return (&/|list (&/T (&/V "jvm-throw" =ex) &type/$Void)))))

(do-template [<name> <tag>]
  (defn <name> [analyse exo-type ?monitor]
    (|do [=monitor (analyse-1+ analyse ?monitor)
          _ (ensure-object =monitor)
          :let [output-type &type/Unit]
          _ (&type/check exo-type output-type)]
      (return (&/|list (&/T (&/V <tag> =monitor) output-type)))))

  analyse-jvm-monitorenter "jvm-monitorenter"
  analyse-jvm-monitorexit  "jvm-monitorexit"
  )

(do-template [<name> <tag> <from-class> <to-class>]
  (let [output-type (&/V "lux;DataT" <to-class>)]
    (defn <name> [analyse exo-type ?value]
      (|do [=value (&&/analyse-1 analyse (&/V "lux;DataT" <from-class>) ?value)
            _ (&type/check exo-type output-type)]
        (return (&/|list (&/T (&/V <tag> =value) output-type))))))

  analyse-jvm-d2f "jvm-d2f" "java.lang.Double"  "java.lang.Float"
  analyse-jvm-d2i "jvm-d2i" "java.lang.Double"  "java.lang.Integer"
  analyse-jvm-d2l "jvm-d2l" "java.lang.Double"  "java.lang.Long"

  analyse-jvm-f2d "jvm-f2d" "java.lang.Float"   "java.lang.Double"
  analyse-jvm-f2i "jvm-f2i" "java.lang.Float"   "java.lang.Integer"
  analyse-jvm-f2l "jvm-f2l" "java.lang.Float"   "java.lang.Long"

  analyse-jvm-i2b "jvm-i2b" "java.lang.Integer" "java.lang.Byte"
  analyse-jvm-i2c "jvm-i2c" "java.lang.Integer" "java.lang.Character"
  analyse-jvm-i2d "jvm-i2d" "java.lang.Integer" "java.lang.Double"
  analyse-jvm-i2f "jvm-i2f" "java.lang.Integer" "java.lang.Float"
  analyse-jvm-i2l "jvm-i2l" "java.lang.Integer" "java.lang.Long"
  analyse-jvm-i2s "jvm-i2s" "java.lang.Integer" "java.lang.Short"

  analyse-jvm-l2d "jvm-l2d" "java.lang.Long"    "java.lang.Double"
  analyse-jvm-l2f "jvm-l2f" "java.lang.Long"    "java.lang.Float"
  analyse-jvm-l2i "jvm-l2i" "java.lang.Long"    "java.lang.Integer"
  )

(do-template [<name> <tag> <from-class> <to-class>]
  (let [output-type (&/V "lux;DataT" <to-class>)]
    (defn <name> [analyse exo-type ?value]
      (|do [=value (&&/analyse-1 analyse (&/V "lux;DataT" <from-class>) ?value)
            _ (&type/check exo-type output-type)]
        (return (&/|list (&/T (&/V <tag> =value) output-type))))))

  analyse-jvm-iand  "jvm-iand"  "java.lang.Integer" "java.lang.Integer"
  analyse-jvm-ior   "jvm-ior"   "java.lang.Integer" "java.lang.Integer"

  analyse-jvm-land  "jvm-land"  "java.lang.Long"    "java.lang.Long"
  analyse-jvm-lor   "jvm-lor"   "java.lang.Long"    "java.lang.Long"
  analyse-jvm-lxor  "jvm-lxor"  "java.lang.Long"    "java.lang.Long"

  analyse-jvm-lshl  "jvm-lshl"  "java.lang.Long"    "java.lang.Integer"
  analyse-jvm-lshr  "jvm-lshr"  "java.lang.Long"    "java.lang.Integer"
  analyse-jvm-lushr "jvm-lushr" "java.lang.Long"    "java.lang.Integer"
  )

(defn analyse-jvm-program [analyse ?args ?body]
  (|let [[_module _name] ?args]
    (|do [=body (&/with-scope ""
                  (&&env/with-local (str _module ";" _name) (&/V "lux;AppT" (&/T &type/List &type/Text))
                    (&&/analyse-1 analyse (&/V "lux;AppT" (&/T &type/IO &type/Unit)) ?body)))]
      (return (&/|list (&/V "jvm-program" =body))))))
