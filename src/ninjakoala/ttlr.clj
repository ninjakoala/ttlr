(ns ninjakoala.ttlr
  (:require [clojure.tools.logging :refer [debug error info warn]]
            [overtone.at-at :as at-at]))

(defonce ^:private items
  (atom {}))

(defonce ^:private pool
  (atom nil))

(defn- get-item
  [name]
  (get @items name))

(defn- refresh
  [name refresh-fn]
  (try
    (if-let [item (get-item name)]
      (let [state-atom (:state item)
            state (refresh-fn)]
        (reset! state-atom state))
      (error "Cannot find slot for" name))
    (catch Exception e
      (error e "Failure while refreshing" name))))

(defn state
  [name]
  (when-let [item (get-item name)]
    @(:state item)))

(defn state-keys
  []
  (keys @items))

(defn unschedule
  [name]
  (when-let [existing (get-item name)]
    (debug "Unscheduling" name)
    (let [kill-result (at-at/kill (:job existing))]
      (dissoc @items name)
      kill-result)))

(defn- unschedule-all
  []
  (doseq [state-key (state-keys)]
    (unschedule state-key)))

(defn schedule
  ([name refresh-fn refresh-millis]
     (schedule name refresh-fn refresh-millis nil))
  ([name refresh-fn refresh-millis initial-value]
     (unschedule name)
     (debug "Scheduling with key" name "to refresh every" refresh-millis "millis")
     (let [job (at-at/every refresh-millis #(refresh name refresh-fn) @pool :initial-delay (if initial-value refresh-millis 1000))]
       (swap! items assoc name {:job job
                                :state (atom initial-value)})
       true)))

(defn init
  [& options]
  (reset! pool (apply at-at/mk-pool options))
  true)

(defn stop
  []
  (unschedule-all))
