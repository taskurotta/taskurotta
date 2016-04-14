#!/bin/bash
set -e
# todo: change path to taskurotta.jar from assemvle jar
export MVN_PROJECT_VERSION=$(cd .. && mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate \
    -Dexpression=project.version |grep -Ev '(^\[|Download\w+:)')

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
    # fixme: we need play on dynamically created playbook
    echo "$2" > /tmp/playbook
    ansible-playbook  -i inventories/local_servers \
        -e taskurotta_jar=$(pwd)/../taskurotta/target/taskurotta-$MVN_PROJECT_VERSION.jar \
        --extra-vars "@extra_vars.json" \
        --tags $1 /tmp/playbook
}

do_start()
{
    if [ ! -n "$(docker network ls| grep autotest)" ];
        then docker network create autotest
    fi
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

    if [ -n "$(docker network ls| grep autotest)" ];
        then docker network rm autotest
    fi
}

do_tail()
{
    (tail -f -n 10000 data/tsk_ff_pusher/pusher.log & \
    grep --line-buffered "ERROR" <(tail -f -n 10000 data/tsk-node1/service.log) & \
    grep --line-buffered "ERROR" <(tail -f -n 10000 data/tsk-node2/service.log) & \
    grep --line-buffered "totalRate:" <(tail -f -n 10000 data/tsk_ff_actors/actors.log)) | tee /tmp/output
}

do_tail_hz()
{
    (grep --line-buffered "com.hazelcast" <(tail -f -n 10000 data/tsk-node1/service.log) & \
    grep --line-buffered "com.hazelcast" <(tail -f -n 10000 data/tsk-node2/service.log)) | tee /tmp/output
}



do_errors()
{
    set +e
    if [ -f data/tsk_ff_pusher/pusher.log ]; then grep "ERROR" data/tsk_ff_pusher/pusher.log; fi
    if [ -f data/tsk-node1/service.log ]; then grep "ERROR" data/tsk-node1/service.log; fi
    if [ -f data/tsk-node2/service.log ]; then grep "ERROR" data/tsk-node2/service.log; fi
    if [ -f data/tsk_ff_actors/actors.log ]; then grep "ERROR" data/tsk_ff_actors/actors.log; fi
    set -e
}

do_prepare()
{
    echo "Pulling docker images. It can take several minutes"
    docker pull java:openjdk-8u66-jdk
    docker pull nginx:1.9.6
    docker pull mongo:2.6.9
    echo ">> done"
}


case "$1" in
    prepare)
        do_prepare
    ;;
    start)
        do_start
    ;;
    tail)
        do_tail
    ;;
    tail-hz)
        do_tail_hz
    ;;
    stop)
        do_stop
    ;;
    clean)
        do_clean
    ;;
    errors)
        do_errors
    ;;
    *)
        echo "Usage: test {prepare | start | stop | clean | errors}" >&2
        exit 3
    ;;

esac