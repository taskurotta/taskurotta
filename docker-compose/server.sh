#!/bin/bash
set -e

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
    docker-compose up -d
}

do_stop()
{
    docker-compose stop
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
        start)
            do_clean_docker
            do_clean_data
            do_up
        ;;
        restart)
            do_stop
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