#!/bin/sh

for pid in $(ps -ef | awk '/run.py/ {print $2}' | head -n -1 )
    do
        echo $pid
        kill -9 $pid
    done
