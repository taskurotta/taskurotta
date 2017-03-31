#!/bin/bash
set -e

JAR=$(ls ../taskurotta/target/taskurotta-*.jar| grep -v javadoc| grep -v sources)
echo "Copy new version of jar to docker directory: $JAR"
cp $JAR ./taskurotta.jar
echo ">> done"

docker build -t taskurotta/server:latest .