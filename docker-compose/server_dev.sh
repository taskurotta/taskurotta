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
    docker-compose -f docker-compose.yml -f docker-compose_dev.yml up -d
}

do_stop()
{
    docker-compose -f docker-compose.yml -f docker-compose_dev.yml stop
}

do_e2e() {
    docker run --name=tsk_e2e -it --rm --net=taskurotta -v /dev/shm:/dev/shm \
        -v $(cd "../dropwizard/src/main/e2e/"; pwd):/protractor \
        -v $(pwd)/data/e2e:/tmp/e2e taskurotta/protractor conf.js --report-dir=/tmp/e2e/report
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
        e2e)
            do_e2e
        ;;
        *)
            echo "Usage: $0 {clean-data|clean-docker|clean|up|stop}" >&2
            exit 3
        ;;

    esac
done
