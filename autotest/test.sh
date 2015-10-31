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
        *)
            echo "Usage: test {build|clean-data|clean-docker|clean|jar}" >&2
            exit 3
        ;;

    esac
done