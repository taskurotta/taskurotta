#bash

java -Xmx128m -Xms128m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=target -cp `ls ../taskurotta/assemble/target/assemble-*.jar| grep -v javadoc| grep -v sources` ru.taskurotta.bootstrap.Main -f actors1.yml
