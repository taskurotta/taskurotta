start java -DassetsMode=dev -Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -jar target/assemble-0.10.5-SNAPSHOT.jar server src/main/resources/hz-ora-mongo.yml