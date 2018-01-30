(ns ewan.runner
    (:require [doo.runner :refer-macros [doo-tests]]
              [ewan.core-test]))

(doo-tests 'ewan.core-test)
