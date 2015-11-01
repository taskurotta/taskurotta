#!/bin/bash
set -e

do_build()
{
    echo "Pulling docker images. It can take several minutes"
    docker pull java:openjdk-8u66-jdk
    docker pull nginx:1.9.5
    docker pull mongo:2.6.9
    echo ">> done"
}

do_clean_data()
{
    echo "Remove files from data/?/* directories (except jar file)..."
    for v in $(ls data/ | grep -v jar); do sudo rm -rf ./data/$v; done
    echo ">> done"
}

do_clean_docker()
{
    echo "Deleting stopped containers"
    STOPPED_DOCKERS=$(docker ps -a --filter status=exited -q)
    if [ -n "$STOPPED_DOCKERS" ];
        then docker rm $STOPPED_DOCKERS && echo ">> done"
        else echo ">> no containers found"
    fi

    echo "Deleting untagged images"
    UNTAGGED_IMAGES=$(docker images -q -f dangling=true)
    if [ -n "$UNTAGGED_IMAGES" ];
        then docker rmi $UNTAGGED_IMAGES && echo ">> done"
        else echo ">> no images found"
    fi
}

do_jar()
{
    JAR=$(ls ../assemble/target/assemble-*.jar| grep -v javadoc| grep -v sources)
    echo "Copying new version of jar to autotest: $JAR"
    mkdir -p ./data/jar
    cp $JAR data/jar/taskurotta.jar
    echo ">> done"
}

# $1 - file path
# $2 - log string
f_wait_log_message()
{
    while [ ! -f $1 ]
    do
      sleep 1
    done

    if [ -n "$2" ];
        then grep -q -m 1 "$2" <(tail -f -n 10000 $1)
    fi
}

do_up()
{
    docker-compose up -d tsk_mongodb
    echo "Waiting tsk_mongodb..."
    f_wait_log_message data/mongo/mongodb.log "waiting for connections on port"
    echo ">> done"

    docker-compose up -d tsk_node1
    echo "Waiting tsk_node1..."
    f_wait_log_message data/node1/service.log "Members \[1\] {"
    echo ">> done. Member is started"
    f_wait_log_message data/node1/service.log "POST    /tasks/gc"
    echo ">> done. And ready for connections"

    docker-compose up -d tsk_node2
    echo "Waiting tsk_node2..."
    f_wait_log_message data/node1/service.log "Members \[2\] {"
    echo ">> done. Member is started and connected to cluster"
    f_wait_log_message data/node1/service.log "POST    /tasks/gc"
    echo ">> done. And ready for connections"

    docker-compose up -d tsk_nginx
    echo "Waiting tsk_nginx..."
    f_wait_log_message data/nginx/access.log
    echo ">> done."

    docker-compose up -d tsk_actors
    echo "Waiting tsk_actors..."
    f_wait_log_message data/actors/actors.log "Bootstrap - \[2\] actors started..."
    echo ">> done."

    docker-compose up -d tsk_pusher
    echo "Waiting tsk_pusher..."
    f_wait_log_message data/pusher/pusher.log "ProcessPusher - process pusher"
    echo ">> done."
}

do_stop()
{
    docker-compose stop
}

for var in "$@"
do
    case "$var" in
        build)
            do_build
            ;;
        clean-data)
            do_clean_data
            ;;
        clean-docker)
            do_clean_docker
            ;;
        clean)
            do_clean_docker
            do_clean_data
            ;;
        jar)
            do_jar
            ;;
        up)
            do_clean_docker
            do_clean_data
            do_up
        ;;
        stop)
            do_stop
        ;;
        *)
            echo "Usage: test {build|clean-data|clean-docker|clean|jar|up|stop}" >&2
            exit 3
        ;;

    esac
done