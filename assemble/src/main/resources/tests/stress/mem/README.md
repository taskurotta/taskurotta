# Actor + Http + Mock Server

server:
        java -Xmx64m -Xms64m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=~/tmp -DassetsMode=dev -Dts.node.custom
        .name="node1" -Ddw.http.port=8081 -Ddw.http.adminPort=9081 -Ddw.logging.file.currentLogFilename="assemble/target/server1.log" -jar assemble/target/assemble-0.4.0.jar server assemble/src/main/resources/mock.yml

actor:
        java -Xmx64m -Xms64m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=~/tmp -cp assemble/target/assemble-0.4.0.jar:assemble/src/main/resources/default.properties ru.taskurotta.bootstrap.Main -f assemble/src/main/resources/tests/stress/mem/b-localhost-8081.yml


# Actor + Direct + Mock Server

actor:
        java -Xmx64m -Xms64m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=~/tmp -cp assemble/target/assemble-0.4.0.jar:assemble/src/main/resources/default.properties ru.taskurotta.bootstrap.Main -f assemble/src/main/resources/tests/stress/mem/mem-mock.yml

# Actor + Direct + HZ + Mongo

actor:
        java -Xmx64m -Xms64m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=~/tmp -cp assemble/target/assemble-0.4.0.jar:assemble/src/main/resources/default.properties ru.taskurotta.bootstrap.Main -f assemble/src/main/resources/tests/stress/mem/mem.yml