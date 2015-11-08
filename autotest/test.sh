#!/bin/bash
set -e

f_play_order()
{
    PLAY_ORDER=$(pathx=`pwd` && awk '$0="- include: '"$pathx"'/app_"$0".yml"' play-order.txt)
}

f_play_order_reverse()
{
    f_play_order
    PLAY_ORDER=$(tac <(echo "$PLAY_ORDER"))
}

f_play()
{
    ansible-playbook  -i inventories/local_servers \
        -e my_taskurotta_jar=$(pwd)/$(ls ../assemble/target/assemble-*.jar| grep -v javadoc| grep -v sources) \
        --tags $1 <(echo "$2")

#                -e my_mongodb_path=/tmp/tsk_mongodb \

}


do_start()
{
    f_play_order
    f_play start "$PLAY_ORDER"
}

do_stop()
{
    f_play_order_reverse
    f_play stop "$PLAY_ORDER"
}

do_clean()
{
    # We need to cache sudo privileges
    sudo echo ""

    f_play_order_reverse
    f_play clean "$PLAY_ORDER"
}

do_tail()
{
    (tail -f -n 10000 data/tsk_ff_pusher/pusher.log & \
    grep --line-buffered "ERROR" <(tail -f -n 10000 data/tsk_node1/service.log) & \
    grep --line-buffered "ERROR" <(tail -f -n 10000 data/tsk_node2/service.log) & \
    grep --line-buffered "totalRate:" <(tail -f -n 10000 data/tsk_ff_actors/actors.log)) | tee /tmp/output
}

do_prepare()
{
    echo "Pulling docker images. It can take several minutes"
    docker pull java:openjdk-8u66-jdk
    docker pull nginx:1.9.5
    docker pull mongo:2.6.9
    echo ">> done"
}


case "$1" in
    build)
        do_prepare
    ;;
    start)
        do_start
    ;;
    tail)
        do_tail
    ;;
    stop)
        do_stop
    ;;
    clean)
        do_clean
    ;;
    *)
        echo "Usage: test {prepare | start | stop | clean}" >&2
        exit 3
    ;;

esac