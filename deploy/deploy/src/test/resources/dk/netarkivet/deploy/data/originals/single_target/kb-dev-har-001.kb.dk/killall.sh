#!/bin/bash
cd /home/dev/UNITTEST/conf
if [ -e ./kill_harvester_8081.sh ]; then
    ./kill_harvester_8081.sh 
fi
if [ -e ./kill_sidekick_8081.sh ]; then
    ./kill_sidekick_8081.sh 
fi
if [ -e ./kill_harvester_8082.sh ]; then
    ./kill_harvester_8082.sh 
fi
if [ -e ./kill_sidekick_8082.sh ]; then
    ./kill_sidekick_8082.sh 
fi
if [ -e ./kill_harvester_8083.sh ]; then
    ./kill_harvester_8083.sh 
fi
if [ -e ./kill_sidekick_8083.sh ]; then
    ./kill_sidekick_8083.sh 
fi

