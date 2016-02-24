#!/bin/sh
java -Xmx128m -Xms128m -XX:+UnlockCommercialFeatures -XX:+FlightRecorder -XX:HeapDumpPath=/tmp/taskurotta-server-heap.hprof -Dcom.sun.management.jmxremote.port=9999 -Dhazelcast.backpressure.enabled=true -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -jar target/assemble-0.8.0-SNAPSHOT.jar server src/main/resources/hz-mongo.yml
