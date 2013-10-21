#!/bin/sh
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar ./target/recipes-*.jar -r ru/taskurotta/recipes/stress/wf-config-jersey.yml
