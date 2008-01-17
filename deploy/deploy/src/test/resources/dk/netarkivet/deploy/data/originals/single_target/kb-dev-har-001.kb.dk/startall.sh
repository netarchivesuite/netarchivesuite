#!/bin/bash
cd /home/dev/UNITTEST/conf
if [ -e ./start_harvester_8081.sh ]; then
    ./start_harvester_8081.sh 
fi
if [ -e ./start_sidekick_8081.sh ]; then
    ./start_sidekick_8081.sh 
fi
if [ -e ./start_harvester_8082.sh ]; then
    ./start_harvester_8082.sh 
fi
if [ -e ./start_sidekick_8082.sh ]; then
    ./start_sidekick_8082.sh 
fi
if [ -e ./start_harvester_8083.sh ]; then
    ./start_harvester_8083.sh 
fi
if [ -e ./start_sidekick_8083.sh ]; then
    ./start_sidekick_8083.sh 
fi

