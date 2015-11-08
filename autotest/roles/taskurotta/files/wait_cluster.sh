#!/bin/bash

while [ ! -f $1 ]
do
  sleep 1
done

if [ -n "$2" ];
    then grep -q -m 1 "Members \[$2\]" <(tail -f -n 10000 $1)
fi