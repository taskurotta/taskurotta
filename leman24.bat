java -Xmx256m -Xms256m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/dump -server -cp assemble/target/assemble-0.7.0-SNAPSHOT.jar ru.taskurotta.bootstrap.Main -f test/src/main/resources/ru/taskurotta/test/time/conf.yml