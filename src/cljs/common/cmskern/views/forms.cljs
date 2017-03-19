(ns cmskern.views.forms
  (:require-macros [cmskern.ui.core :refer [handler-fn]])
  (:require
            [reagent.core :as r]

            [cmskern.views.utils :refer [deref-or-value]]
            ))


(defn- input-text-base
  "Returns markup for a basic text input label"
  [& {:keys [model input-type wrapper] :or {wrapper identity} :as args}]
  (print ::input-text-base input-type args)
  (let [
        ;; Holds the last known external value of model, to detect external model changes
        external-model (r/atom (deref-or-value model))
        ;; Create a new atom from the model to be used internally (avoid nil)
        internal-model (r/atom (if (nil? @external-model) "" @external-model))
        input-elem (case input-type
                     :text :input
                     :textarea :textarea
                     :password :input
                     :input)]
    (fn
      [& {:keys [model status status-icon? status-tooltip placeholder rows on-change change-on-blur? validation-regex disabled? class style attr]
          :or   {change-on-blur? true
                 }
          :as   args}]
      (let [latest-ext-model (deref-or-value model)
            disabled?        (deref-or-value disabled?)
            change-on-blur?  (deref-or-value change-on-blur?)
            showing?         (r/atom false)]
        (when (not= @external-model latest-ext-model) ;; Has model changed externally?
          (reset! external-model latest-ext-model)
          (reset! internal-model latest-ext-model))
        (wrapper
         [:div.vcomp.input-text
          [:div
           {:class (str "inner "          ;; form-group
                        (case status
                          :success "has-success "
                          :warning "has-warning "
                          :error "has-error "
                          "")
                        (when (and status status-icon?) "has-feedback"))
            }
           [input-elem
            (merge
             {
              :type        input-type
              :rows        (when (= input-type :textarea) (if rows rows 3))
              :placeholder placeholder
              :value       @internal-model
              :disabled    disabled?
              :on-change   (handler-fn
                            (let [new-val (-> event .-target .-value)]
                              (when (and
                                     on-change
                                     (not disabled?)
                                     (if validation-regex (re-find validation-regex new-val) true))
                                (reset! internal-model new-val)
                                (when-not change-on-blur?
                                  (on-change @internal-model)))))
              :on-blur     (handler-fn
                            (when (and
                                   on-change
                                   change-on-blur?
                                   (not= @internal-model @external-model))
                              (on-change @internal-model)))
              :on-key-up   (handler-fn
                            (if disabled?
                              (.preventDefault event)
                              (case (.-which event)
                                13 (when on-change (on-change @internal-model))
                                27 (reset! internal-model @external-model)
                                true)))

              }
             attr)]
           (when (and status-icon? status)
             (let [icon-class (case status :success "zmdi-check-circle" :warning "zmdi-alert-triangle" :error "zmdi-alert-circle zmdi-spinner" :validating "zmdi-hc-spin zmdi-rotate-right zmdi-spinner")]
               (println ::inner-status-icon-check status-icon? status icon-class)
               [:span {:class         (str "zmdi zmdi-hc-fw " icon-class " form-control-feedback")
                       :style         {:position "static"
                                       :height   "auto"
                                       :opacity  (if (and status-icon? status) "1" "0")}
                       }]

               (when status-tooltip
                 [:div.help-block status-tooltip]
                 )))]])))))

(defn input-password
  [& args]
  (apply input-text-base :input-type :password args))

(defn input-text
  [& args]
  (println ::input-text args)
  (apply input-text-base :input-type :text args))


(defn input-textarea
  [& args]
  (apply input-text-base :input-type :textarea args))


(defn input-control
  "Input-Field mit Label und Wrapper"
  [& {:keys [class label input-type]
      :or {class "pure-control-group"
           input-type :text}
      :as args}]
  (apply input-text-base :wrapper #(vector :div {:class class}
                                  [:label nil label]
                                  %
                                  ) (flatten (into [] args)))
  )
