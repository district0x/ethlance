#!/bin/bash
# Clojurescript's Test Runner runs in an asychronous manner that makes
# it difficult to retrieve the error code. This script runs the test
# runner and determines whether the test cases have failed after
# completion and produces the correct error code.
# Notes:
#
# - tee /dev/tty will continue to output to stdout, while also
#   carrying out the additional piping operations.

lein doo node "test-server" once | tee /dev/tty | tail -1 | grep "0 failures, 0 errors"
if [ $? -eq 0 ]; then
    exit 0
else
    exit 1
fi
