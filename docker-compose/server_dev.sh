#!/bin/bash
set -e

export MVN_PROJECT_VERSION=$(cd .. && mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate \
    -Dexpression=project.version |grep -Ev '(^\[|Download\w+:)')

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

    if [ -n "$(docker network ls| grep taskurotta)" ];
        then docker network rm taskurotta
    fi
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
    if [ ! -n "$(docker network ls| grep taskurotta)" ];
        then docker network create taskurotta
    fi

    docker-compose -f docker-compose.yml -f docker-compose_dev.yml up -d --no-deps tsk_mongodb
    echo "Waiting tsk_mongodb..."
    f_wait_log_message data/mongo/mongodb.log "waiting for connections on port"
    echo ">> done"

    docker-compose -f docker-compose.yml -f docker-compose_dev.yml up -d --no-deps tsk_node1
    echo "Waiting tsk_node1..."
    f_wait_log_message data/node1/service.log "Members \[1\] {"
    echo ">> done. Member is started"
    f_wait_log_message data/node1/service.log "POST    /tasks/gc"
    echo ">> done. And ready for connections"

    docker-compose -f docker-compose.yml -f docker-compose_dev.yml up -d --no-deps tsk_node2
    echo "Waiting tsk_node2..."
    f_wait_log_message data/node1/service.log "Members \[2\] {"
    echo ">> done. Member is started and connected to cluster"
    f_wait_log_message data/node1/service.log "POST    /tasks/gc"
    echo ">> done. And ready for connections"

    docker-compose -f docker-compose.yml -f docker-compose_dev.yml up -d --no-deps tsk_web_dev
    echo "Python web server started for static web files"

    docker-compose -f docker-compose.yml -f docker-compose_dev.yml up -d --no-deps tsk_web_doc_dev
    echo "Python web server started for documentation files"

    docker-compose -f docker-compose.yml -f docker-compose_dev.yml up -d --no-deps tsk_http
    echo "HA proxy started on 80 port"

}

do_stop()
{
    docker-compose -f docker-compose.yml -f docker-compose_dev.yml stop
}

for var in "$@"
do
    case "$var" in
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
        up)
            do_clean_docker
            do_clean_data
            do_up
        ;;
        stop)
            do_stop
        ;;
        *)
            echo "Usage: $0 {clean-data|clean-docker|clean|up|stop}" >&2
            exit 3
        ;;

    esac
done
