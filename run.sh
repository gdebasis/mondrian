#!/bin/bash

if [ $# -lt 3 ] 
then
    echo "usage: $0 <n> <maxdepth> <prob. of exploring along bad state>"
    exit
fi

mvn exec:java@solver -Dexec.args="$1 $2 $3"
open -a "Google Chrome.app" solutions/mondrian-$1-$1.htm
