(ns ewan.runner
    (:require [doo.runner :refer-macros [doo-tests]]
              [ewan.core-test]
              [ewan.eaf30-test]))

(doo-tests 'ewan.core-test)
(doo-tests 'ewan.eaf30-test)
