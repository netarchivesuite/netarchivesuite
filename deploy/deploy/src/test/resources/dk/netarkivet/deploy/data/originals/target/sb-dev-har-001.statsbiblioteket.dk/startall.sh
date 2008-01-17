#!/bin/bash
cd /home/netarkiv/UNITTEST/conf
if [ -e ./start_harvester_8081.sh ]; then
    ./start_harvester_8081.sh 
fi
if [ -e ./start_sidekick_8081.sh ]; then
    ./start_sidekick_8081.sh 
fi

