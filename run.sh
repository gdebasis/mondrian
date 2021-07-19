#!/bin/bash

if [ $# -lt 4 ] 
then
    echo "usage: $0 <n> <max-depth> <max-states-to-visit> <beam-size>"
    exit
fi

cat > init.properties << EOF1
maxdepth=$2
maxstates_to_visit=$3
beamsize=$4
sampling=biased
EOF1

mvn exec:java@solver -Dexec.args="$1 init.properties"
#open -a "Google Chrome.app" solutions/mondrian-$1-$1.htm

