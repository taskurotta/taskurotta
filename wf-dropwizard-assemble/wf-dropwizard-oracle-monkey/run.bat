start java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005 -javaagent:./target/dependency/aspectjweaver-1.6.7.jar -jar target/wf-dropwizard-oracle-monkey-0.2.0-SNAPSHOT.jar server target/classes/TaskQueueConfig.yml