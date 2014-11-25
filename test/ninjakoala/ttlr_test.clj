(ns ninjakoala.ttlr-test
  (:require [midje
             [sweet :refer :all]
             [util :refer [testable-privates]]]
            [ninjakoala.ttlr :refer :all]
            [overtone.at-at :as at-at]))

(fact "that our refresh mechanism works"
      (do (#'ninjakoala.ttlr/refresh* :name (fn [] "new value"))
          (state :name)) => "new value"
      (provided
       (#'ninjakoala.ttlr/get-item :name) => {:state (atom nil)}))

(fact "that getting the state of something which doesn't exist is nil"
      (state :name) => nil
      (provided
       (#'ninjakoala.ttlr/get-item :name) => nil))

(fact "that getting the state of something which does exist returns the value"
      (state :name) => ..value..
      (provided
       (#'ninjakoala.ttlr/get-item :name) => {:state (atom ..value..)}))

(fact "that unscheduling does the right thing"
      (do (#'ninjakoala.ttlr/unschedule-all)
          (unschedule :name)
          (count (state-keys))) => 0
      (provided
       (#'ninjakoala.ttlr/get-item :name) => {:job ..job..}
       (at-at/kill ..job..) => true))

(fact "that scheduling with an initial value does the right thing"
      (schedule :name ..refresh-fn.. ..refresh-millis.. ..initial-value..)
      => truthy
      (provided
       (unschedule :name) => false
       (at-at/every ..refresh-millis.. anything anything :initial-delay ..refresh-millis..) => ..job..))

(fact "that scheduling with an initial value waits a second"
      (schedule :name ..refresh-fn.. ..refresh-millis..)
      => truthy
      (provided
       (unschedule :name) => false
       (at-at/every ..refresh-millis.. anything anything :initial-delay 1000) => ..job..))

(fact "that initialising with no options uses defaults to create the pool"
      (init) => truthy
      (provided
       (at-at/mk-pool) => ..pool..))

(fact "that initialising uses the correct options to create the pool"
      (init :cpu-count 5) => truthy
      (provided
       (at-at/mk-pool :cpu-count 5) => ..pool..))
