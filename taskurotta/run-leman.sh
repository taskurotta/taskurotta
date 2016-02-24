#!/bin/sh

java -Xmx88m -Xms88m -XX:+HeapDumpOnOutOfMemoryError -XX:+UnlockCommercialFeatures -XX:+FlightRecorder -XX:HeapDumpPath=/tmp/leman-heap.hprof -server -cp target/assemble-0.8.0-SNAPSHOT.jar:assemble/src/main/resources/default.properties ru.taskurotta.bootstrap.Main -f src/main/resources/tests/stress/mem/leman.yml