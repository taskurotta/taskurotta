start java -Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -DassetsMode=dev -Dts.oradb.url=jdbc:oracle:thin:@10.133.9.198:1521:tsqs -Dts.oradb.user=taskurotta -Dts.oradb.password=taskurotta -Ddw.http.port=8811 -Ddw.http.adminPort=8812 -Ddw.logging.file.currentLogFilename="./target/logs/service1.log" -jar target/assemble-0.7.0-SNAPSHOT.jar server src/main/resources/hz-ora-mongo.yml