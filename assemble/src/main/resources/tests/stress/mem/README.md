Run commands at the root directory of project


# Actor + Http + Mock Server

server:
        java -Xmx64m -Xms64m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp -DassetsMode=dev -Dts.node.custom
        .name="node1" -Ddw.http.port=8081 -Ddw.http.adminPort=9081 -Ddw.logging.file.currentLogFilename="assemble/target/server1.log" -jar assemble/target/assemble-0.5.0-SNAPSHOT.jar server assemble/src/main/resources/mock.yml

actor:
        java -Xmx64m -Xms64m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp -cp assemble/target/assemble-0.5.0-SNAPSHOT.jar:assemble/src/main/resources/default.properties ru.taskurotta.bootstrap.Main -f assemble/src/main/resources/tests/stress/mem/b-localhost-8081.yml


# Actor + Direct + Mock Server

actor:
        java -Xmx64m -Xms64m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp -cp assemble/target/assemble-0.5.0-SNAPSHOT.jar:assemble/src/main/resources/default.properties ru.taskurotta.bootstrap.Main -f assemble/src/main/resources/tests/stress/mem/mem-mock.yml

# Actor + Direct + HZ + Mongo

actor:
        java -Xmx88m -Xms88m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp -cp assemble/target/assemble-0.5.0-SNAPSHOT.jar:assemble/src/main/resources/default.properties ru.taskurotta.bootstrap.Main -f assemble/src/main/resources/tests/stress/mem/mem.yml

# Full feature test: Actor + Direct + HZ + Mongo

actor:
        java -Xmx88m -Xms88m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp -server -cp
        assemble/target/assemble-0.5.0-SNAPSHOT.jar:assemble/src/main/resources/default.properties ru.taskurotta.bootstrap.Main -f assemble/src/main/resources/tests/stress/mem/mem-ff.yml

# Full feature test with monkeys: Actor + Direct + HZ + Mongo
                java -Xmx128m -Xms128m -server -javaagent:assemble/target/dependency/aspectjweaver-1.7.3.jar -cp \
                assemble/target/assemble-0.5.0-SNAPSHOT.jar:assemble/src/main/resources/default.properties \
                ru.taskurotta.bootstrap.Main -f assemble/src/main/resources/tests/stress/mem/mem-ff.yml