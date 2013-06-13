#!/bin/sh
java -Ddw.http.port=8080 -Ddw.http.adminPort=8081 -Ddw.logging.file.currentLogFilename="./target/logs/service1.log" -jar target/wf-dropwizard-hz-poc-0.1.0-SNAPSHOT.jar server src/main/resources/conf.yml
