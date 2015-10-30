#bash

java -Xmx64m -Xms64m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=target -Dlogback.configurationFile=pusher-log.xml -Dtsk.endpoint=http://taskurotta.local:8810 -Dhz.nodes=localhost:7777,localhost:7778 -Dtest.ff.fixedPushRate=false -Dtest.ff.minQueuesSize=1000 -Dtest.ff.maxQueuesSize=2000 -cp `ls ../taskurotta/assemble/target/assemble-*.jar| grep -v javadoc| grep -v sources` ru.taskurotta.test.stress.ProcessPusher file:pusher.xml
