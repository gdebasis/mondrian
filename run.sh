#!/bin/bash

if [ $# -lt 5 ] 
then
    echo "usage: $0 <n> <max-depth> <max-states-to-visit> <max-states-to-explore> <beam-size>"
    exit
fi

cat > init.properties << EOF1
maxdepth=$2
maxqueue_size=$3
maxstates_to_explore=$4
beamsize=$5
sampling=uniform
gen.spiral=true
EOF1

mvn exec:java@solver -Dexec.args="$1 init.properties"
#open -a "Google Chrome.app" solutions/mondrian-$1-$1.htm

