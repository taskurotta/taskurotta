#!/bin/bash
set -e

# $1 - node number
# $2 - sleep seconds
f_stop_node_and_sleep()
{
    LOG_FILE="data/tsk_node$1/service.log"

    echo "stop container tsk_node$1"
    docker stop "tsk_node$1"

    #echo "count lines of log file (LOLF). It makes possible to skip them later"
    eval "LAST_LOLF_$1=$(wc -l < ${LOG_FILE})"
    var="LAST_LOLF_$1"

    echo "sleep $2 second(s)..."
    sleep $2
}

# $1 - wait node number
# $2 - desired cluster size
f_start_node()
{

    LOG_FILE="data/tsk_node$1/service.log"

    echo "start container tsk_node$1"
    docker start "tsk_node$1"

    # wait until file will be created
    while [ ! -f "$LOG_FILE" ]
    do
        echo "waiting 1 second until file will be created"
        sleep 1
    done

    var="LAST_LOLF_$1"
    LAST_LOLF=$(echo "${!var}")

    START_TIME=$(date +%s)
    echo "wait until cluster will have $2 members. Skip ${!var} lines in tsk_node$1 log file"
    grep -q -m 1 "Members \[$2\]" <(tail -f -n +${LAST_LOLF} ${LOG_FILE})
    echo "Done in $(($(date +%s) - $START_TIME)) seconds"
}

f_wait_actors_work()
{
    ACTORS_LOLF=$(wc -l < data/tsk_ff_actors/actors.log)
    echo "wait until actors start to process tasks. Skip $ACTORS_LOLF lines in actors log file"
    grep -q -m 1 "tps; totalRate" <(tail -f -n +${ACTORS_LOLF} data/tsk_ff_actors/actors.log)
}

for i in {1..30}
do
    echo ""
    echo "Cluster will be broken. $i times"

    f_stop_node_and_sleep 1 1
    f_start_node 1 2
    #f_wait_actors_work

    echo "give it to work normally around 20 seconds..."
    sleep 20

    f_stop_node_and_sleep 2 1
    f_start_node 2 2
    #f_wait_actors_work

    echo "give it to work normally around 20 seconds..."
    sleep 20

done

