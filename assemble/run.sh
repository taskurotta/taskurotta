#!/bin/sh
java -Dcom.sun.management.jmxremote.port=9998 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -DassetsMode=dev -Dts.node.custom-name="node2" -Ddw.http.port=8811 -Ddw.http.adminPort=8812 -Ddw.logging.file.currentLogFilename="./target/logs/service2.log" -jar target/assemble-0.3.0-SNAPSHOT.jar server src/main/resources/hz-mongo.yml
