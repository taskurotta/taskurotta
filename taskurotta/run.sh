#!/bin/sh
java -Dcom.sun.management.jmxremote.port=9998 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -jar target/assemble-0.8.0-SNAPSHOT.jar server src/main/resources/hz-mongo.yml
