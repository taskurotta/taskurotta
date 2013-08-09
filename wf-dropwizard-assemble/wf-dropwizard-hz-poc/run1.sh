java -Dts.hz.port=5701 -Xmx512m -Ddw.http.port=8811 -jar target/wf-dropwizard-hz-poc-0.2.0-SNAPSHOT.jar server src/main/resources/conf.yml
#java -Dts.hz.port=5701 -Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -DassetsMode=dev -Xmx512m -Ddw.http.port=8811 -Ddw.http.adminPort=8812 -Ddw.logging.file.currentLogFilename="./target/logs/service1.log" -javaagent:./target/dependency/aspectjweaver-1.7.2.jar -jar target/wf-dropwizard-hz-poc-0.2.0-SNAPSHOT.jar server src/main/resources/conf.yml
#-javaagent:./target/dependency/aspectjweaver-1.7.2.jar
# -Dts.hz.port=7776
