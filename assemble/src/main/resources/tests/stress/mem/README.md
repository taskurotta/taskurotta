Run commands at the root directory of project


# Actor + Http + Mock Server

server:
        java -Xmx64m -Xms64m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp -DassetsMode=dev -Dts.node.custom
        .name="node1" -Ddw.http.port=8081 -Ddw.http.adminPort=9081 -Ddw.logging.file.currentLogFilename="assemble/target/server1.log" -jar assemble/target/assemble-0.5.0-SNAPSHOT.jar server assemble/src/main/resources/mock.yml

actor:
        java -Xmx64m -Xms64m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp -cp assemble/target/assemble-0.7.0-SNAPSHOT.jar:assemble/src/main/resources/default.properties ru.taskurotta.bootstrap.Main -f assemble/src/main/resources/tests/stress/mem/b-localhost-8081.yml


# Actor + Direct + Mock Server

actor:
        java -Xmx64m -Xms64m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp -cp assemble/target/assemble-0.7.0-SNAPSHOT.jar:assemble/src/main/resources/default.properties ru.taskurotta.bootstrap.Main -f assemble/src/main/resources/tests/stress/mem/mem-mock.yml

# Actor + Direct + HZ + Mongo

actor:
        java -Xmx88m -Xms88m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp -cp assemble/target/assemble-0.7.0-SNAPSHOT.jar:assemble/src/main/resources/default.properties ru.taskurotta.bootstrap.Main -f assemble/src/main/resources/tests/stress/mem/mem.yml

# Full feature test: Actor + Direct + HZ + Mongo

actor:
        java -Xmx88m -Xms88m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp -server -cp assemble/target/assemble-0.7.0-SNAPSHOT.jar:assemble/src/main/resources/default.properties ru.taskurotta.bootstrap.Main -f assemble/src/main/resources/tests/stress/mem/mem-ff.yml

# Leman Full feature test: Actor + Direct + HZ + Mongo

actor:
        java -Xmx88m -Xms88m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp -server -cp assemble/target/assemble-0.7.0-SNAPSHOT.jar:assemble/src/main/resources/default.properties ru.taskurotta.bootstrap.Main -f assemble/src/main/resources/tests/stress/mem/leman.yml


# Full feature test with monkeys: Actor + Direct + HZ + Mongo
                java -Xmx128m -Xms128m -server -javaagent:assemble/target/dependency/aspectjweaver-1.7.3.jar -cp \
                assemble/target/assemble-0.7.0-SNAPSHOT.jar:assemble/src/main/resources/default.properties \
                ru.taskurotta.bootstrap.Main -f assemble/src/main/resources/tests/stress/mem/mem-ff.yml

# Full feature test with monkeys: Actor + Direct + HZ + Mongo

## Server
java -Xmx512m -Xms512m -server -javaagent:assemble/target/dependency/aspectjweaver-1.7.3.jar -Ddw.http.port=8081 -Ddw.http.adminPort=9081 -Ddw.logging.file.currentLogFilename="assemble/target/server1.log" -jar assemble/target/assemble-0.6.0-SNAPSHOT.jar server assemble/src/main/resources/hz-mongo.yml

## Client
java -Xmx64m -Xms64m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=~/tmp -cp assemble/target/assemble-0.6.0-SNAPSHOT.jar;assemble/src/main/resources/default.properties ru.taskurotta.bootstrap.Main -f test/src/main/resources/ru/taskurotta/test/fullfeature/conf-jersey-stress.yml


# Test with mongo data loss: HZ + Mongo + Oracle + Actor

## Server
-Dts.gc.enabled=false To prevent removing finished processes from server before client could count them at the test end
-Dts.recovery.enabled=true Ensure recovery is here - it is a recovery test after all
-Dts.recovery.process.incomplete-timeout="5 SECONDS" fast processes expire fast
-Dts.recovery.find-incomplete-process-period="5 SECONDS" rapid process search means test would finish fast
-Dts.hz.queue.task.memory-limit=5 To ensure that only 5 tasks in the queue would survive mongo crash

java -Xmx128m -Xms128m -server -Ddw.http.port=8811 -Dts.gc.enabled=false -Dts.recovery.enabled=true -Dts.recovery.process.incomplete-timeout="5 SECONDS" -Dts.recovery.find-incomplete-process-period="5 SECONDS" -Dts.hz.queue.task.memory-limit=5 -jar assemble/target/assemble-0.6.0-SNAPSHOT.jar server assemble/src/main/resources/hz-ora-mongo.yml

## Client
java -Xmx64m -Xms64m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=~/tmp -cp assemble/target/assemble-0.6.0-SNAPSHOT.jar ru.taskurotta.bootstrap.Main -f test/src/main/resources/ru/taskurotta/test/mongofail/conf.yml